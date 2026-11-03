
package org.egov.finance.migration.modules.banktobank.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.ApiResponse;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.MigrationResult;
import org.egov.finance.migration.common.dto.RecordResult;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.entity.MigrationJobDetail;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.common.enums.RecordStatus;
import org.egov.finance.migration.common.repository.MigrationJobDetailRepository;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferRecord;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferRequest;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferResponse;
import org.egov.finance.migration.modules.banktobank.reader.BankToBankTransferExcelReader;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;
import org.springframework.stereotype.Service;

@Service
public class BankToBankTransferMigrationProcessor extends AbstractMigrationProcessor {

	private final BankToBankTransferExcelReader excelReader;
	private final BankToBankTransferRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final BankToBankTransferApiClient apiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final RequestInfoBuilder requestInfoBuilder;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public BankToBankTransferMigrationProcessor(BankToBankTransferExcelReader excelReader,
			BankToBankTransferRequestBuilder requestBuilder, DuplicateDetectionService duplicateDetectionService,
			BankToBankTransferApiClient apiClient, MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository, RequestInfoBuilder requestInfoBuilder,
			MigrationCancellationManager cancellationManager, MigrationProgressPublisher progressPublisher) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.apiClient = apiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
		this.requestInfoBuilder = requestInfoBuilder;
		this.cancellationManager = cancellationManager;
		this.progressPublisher = progressPublisher;
	}

	@Override
	public MigrationType getMigrationType() {
		return MigrationType.BANK_TO_BANK_TRANSFER;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();

		List<RecordResult> recordResults = new ArrayList<>();

		/*
		 * ============================================================ STEP 1 : READ
		 * EXCEL ============================================================
		 */

		List<BankToBankTransferRecord> records = excelReader.read(request.getFilePath());

		/*
		 * ============================================================ STEP 2 : GET
		 * EXISTING MIGRATION JOB
		 * ============================================================
		 */

		MigrationJob job = migrationJobRepository.findByJobId(request.getJobId())
				.orElseThrow(() -> new IllegalArgumentException("Migration job not found: " + request.getJobId()));

		/*
		 * ============================================================ STEP 3 :
		 * INITIALIZE JOB PROGRESS
		 * ============================================================
		 */

		job.setTotalRecords(records.size());
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");
		job.setCurrentMessage("Excel read successfully. Starting Bank To Bank Transfer migration...");

		migrationJobRepository.save(job);

		/*
		 * ============================================================ COUNTERS
		 * ============================================================
		 */

		int success = 0;
		int failed = 0;
		int skipped = 0;

		/*
		 * ============================================================ STEP 4 : PROCESS
		 * EACH TRANSFER INDEPENDENTLY
		 *
		 * One Excel row = one Bank To Bank Transfer API request.
		 *
		 * If one transfer fails, remaining transfers are processed.
		 * ============================================================
		 */

		for (int i = 0; i < records.size(); i++) {

			if (cancellationManager.isCancelled(request.getJobId())) {

				job.setStatus("CANCELLED");
				job.setCurrentMessage("Migration cancelled by user.");

				migrationJobRepository.saveAndFlush(job);
				progressPublisher.publish(job);

				break;
			}

			BankToBankTransferRecord record = records.get(i);

			long recordStart = System.currentTimeMillis();

			RecordResult result = new RecordResult();

			result.setRecordNumber(i + 1);
			result.setStartRow(record.getRowNumber());
			result.setEndRow(record.getRowNumber());

			/*
			 * ======================================================== DUPLICATE CHECK
			 * ========================================================
			 */

			List<String> recordKeys = getRecordKeys(record);

			boolean alreadyMigrated = duplicateDetectionService.isAlreadyMigrated(request.getTenantId(),
					request.getMigrationType().name(), recordKeys);

			if (alreadyMigrated) {

				result.setStatus(RecordStatus.SKIPPED);
				result.setMessage("Bank To Bank Transfer already migrated.");
				result.setExecutionTime(0L);

				skipped++;

				recordResults.add(result);

				saveMigrationDetail(job, request, result, RecordStatus.SKIPPED.name(), recordKeys);

				updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
						"Bank To Bank Transfer " + (i + 1) + " of " + records.size() + " skipped - already migrated.");

				continue;
			}

			/*
			 * ======================================================== PROCESS CURRENT
			 * TRANSFER ========================================================
			 */

			try {

				/*
				 * Build BankToBankTransferRequest
				 */

				BankToBankTransferRequest transferRequest = requestBuilder.build(record);

				if (transferRequest == null) {

					throw new RuntimeException("Unable to build BankToBankTransferRequest.");
				}

				/*
				 * Build API request
				 */

				ApiRequest<BankToBankTransferRequest> apiRequest = new ApiRequest<>();

				apiRequest.setTenantId(request.getTenantId());
				apiRequest.setRequest(transferRequest);
				apiRequest.setRequestInfo(requestInfoBuilder.build(request.getTenantId()));

				/*
				 * Call Bank To Bank Transfer API
				 */

				ApiResponse<BankToBankTransferResponse> response = apiClient.createBankToBankTransfer(apiRequest);

				/*
				 * Validate API response
				 */

				if (response == null) {

					throw new RuntimeException("Bank To Bank Transfer API returned empty response.");
				}

				if (!response.isSuccess()) {

					throw new RuntimeException(response.getMessage() != null ? response.getMessage()
							: "Bank To Bank Transfer API failed.");
				}

				result.setStatus(RecordStatus.SUCCESS);
				result.setMessage("Bank To Bank Transfer created successfully.");

				success++;

			} catch (Exception e) {

				result.setStatus(RecordStatus.FAILED);

				String errorMessage = getRootCauseMessage(e);

				result.setMessage(errorMessage);

				failed++;

			} finally {

				result.setExecutionTime(System.currentTimeMillis() - recordStart);
			}

			/*
			 * ======================================================== SAVE RESULT
			 * ========================================================
			 */

			recordResults.add(result);

			saveMigrationDetail(job, request, result, result.getStatus().name(), recordKeys);

			/*
			 * ======================================================== UPDATE REALTIME
			 * PROGRESS ========================================================
			 */

			updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
					"Processing Bank To Bank Transfer " + (i + 1) + " of " + records.size());
		}

		/*
		 * ============================================================ STEP 5 : FINAL
		 * JOB STATUS ============================================================
		 */

		if ("CANCELLED".equals(job.getStatus())) {

			job.setCompletedTime(LocalDateTime.now());
			migrationJobRepository.save(job);

			cancellationManager.remove(request.getJobId());

			return MigrationResult.builder().success(false).message("Migration cancelled by user.")
					.totalRecords(records.size()).successRecords(success).failedRecords(failed).skippedRecords(skipped)
					.recordResults(recordResults).totalExecutionTime(System.currentTimeMillis() - startTime).build();
		}

		job.setTotalRecords(records.size());
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setProgressPercent(100);
		job.setCurrentRecord(records.size());

		/*
		 * ============================================================ FINAL MESSAGE
		 * ============================================================
		 */

		String finalMessage;

		if (failed > 0) {

			finalMessage = "Bank To Bank Transfer migration completed with " + failed + " failed record(s).";

		} else if (skipped > 0) {

			finalMessage = "Bank To Bank Transfer migration completed successfully. " + skipped
					+ " record(s) skipped as duplicate.";

		} else {

			finalMessage = "Bank To Bank Transfer migration completed successfully.";
		}

		job.setCurrentMessage(finalMessage);

		/*
		 * ============================================================ FINAL STATUS
		 * ============================================================
		 */

		if (failed > 0) {

			job.setStatus("COMPLETED_WITH_ERRORS");

		} else {

			job.setStatus("COMPLETED");
		}

		job.setCompletedTime(LocalDateTime.now());

		migrationJobRepository.save(job);

		progressPublisher.publish(job);

		/*
		 * ============================================================ TOTAL EXECUTION
		 * TIME ============================================================
		 */

		long totalExecutionTime = System.currentTimeMillis() - startTime;

		/*
		 * ============================================================ RETURN FINAL
		 * RESULT ============================================================
		 */

		return MigrationResult.builder().success(failed == 0).message(finalMessage).totalRecords(records.size())
				.successRecords(success).failedRecords(failed).skippedRecords(skipped).recordResults(recordResults)
				.totalExecutionTime(totalExecutionTime).build();
	}

	/**
	 * Update realtime migration job progress.
	 */
	private void updateJobProgress(MigrationJob job, int currentRecord, int totalRecords, int success, int failed,
			int skipped, String message) {

		job.setCurrentRecord(currentRecord);
		job.setTotalRecords(totalRecords);

		int progress = 0;

		if (totalRecords > 0) {

			progress = (int) (((double) currentRecord / totalRecords) * 100);
		}

		job.setProgressPercent(progress);
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setCurrentMessage(message);

		migrationJobRepository.saveAndFlush(job);

		progressPublisher.publish(job);
	}

	/**
	 * Save migration detail for one Bank To Bank Transfer record.
	 */
	private void saveMigrationDetail(MigrationJob job, MigrationRequest request, RecordResult result, String status,
			List<String> recordKeys) {

		MigrationJobDetail detail = new MigrationJobDetail();

		detail.setJob(job);

		detail.setTenantId(request.getTenantId());

		detail.setModuleCode(request.getMigrationType().name());

		detail.setRecordNumber(result.getRecordNumber());

		detail.setStartRow(result.getStartRow());

		detail.setEndRow(result.getEndRow());

		detail.setStatus(status);

		detail.setMessage(result.getMessage());

		detail.setExecutionTime(result.getExecutionTime());

		detail.setRecordKey(recordKeys);

		detail.setCreatedTime(LocalDateTime.now());

		migrationJobDetailRepository.save(detail);
	}

	/**
	 * Get the actual root cause message.
	 */
	private String getRootCauseMessage(Throwable exception) {

		Throwable root = exception;

		while (root.getCause() != null) {
			root = root.getCause();
		}

		if (root.getMessage() == null) {
			return root.getClass().getSimpleName();
		}

		return root.getMessage();
	}
//
//	/**
//	 * Generate unique keys for Bank To Bank Transfer duplicate detection.
//	 */
//	@Override
//	protected List<String> getRecordKeys(Object record) {
//
//		if (!(record instanceof BankToBankTransferRecord transfer)) {
//
//			throw new IllegalArgumentException("Invalid record type for BankToBankTransferMigrationProcessor");
//		}
//
//		String fromAccount = normalize(transfer.getFromAccountNumber());
//		String toAccount = normalize(transfer.getToAccountNumber());
//		String referenceNumber = normalize(transfer.getReferenceNumber());
//
//		List<String> recordKeys = new ArrayList<>();
//
//		if (!fromAccount.isEmpty() && !toAccount.isEmpty() && !referenceNumber.isEmpty()) {
//
//			recordKeys.add("BANK_TO_BANK_TRANSFER:" + fromAccount + ":" + toAccount + ":" + referenceNumber);
//		}
//
//		if (recordKeys.isEmpty()) {
//
//			throw new IllegalArgumentException("Unable to generate unique record key for Bank To Bank Transfer. "
//					+ "Source account, destination account or reference number is missing.");
//		}
//
//		return recordKeys;
//	}
}
