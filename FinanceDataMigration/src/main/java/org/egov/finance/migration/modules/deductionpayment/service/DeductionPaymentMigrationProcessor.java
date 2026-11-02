package org.egov.finance.migration.modules.deductionpayment.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.MigrationResult;
import org.egov.finance.migration.common.dto.RecordResult;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.entity.MigrationJobDetail;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.common.enums.RecordStatus;
import org.egov.finance.migration.common.repository.MigrationJobDetailRepository;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.modules.deductionpayment.dto.CreateDeductionPaymentRequest;
import org.egov.finance.migration.modules.deductionpayment.dto.DeductionPaymentRecord;
import org.egov.finance.migration.modules.deductionpayment.reader.DeductionPaymentExcelReader;
import org.egov.finance.migration.modules.deductionpayment.response.DeductionPaymentResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;
import org.springframework.stereotype.Service;

@Service
public class DeductionPaymentMigrationProcessor extends AbstractMigrationProcessor {

	private final DeductionPaymentExcelReader excelReader;
	private final DeductionPaymentRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final DeductionPaymentApiClient deductionPaymentApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public DeductionPaymentMigrationProcessor(DeductionPaymentExcelReader excelReader,
			DeductionPaymentRequestBuilder requestBuilder, DuplicateDetectionService duplicateDetectionService,
			DeductionPaymentApiClient deductionPaymentApiClient, MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository, MigrationCancellationManager cancellationManager,
			MigrationProgressPublisher progressPublisher) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.deductionPaymentApiClient = deductionPaymentApiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
		this.cancellationManager = cancellationManager;
		this.progressPublisher = progressPublisher;
	}

	@Override
	public MigrationType getMigrationType() {
		return MigrationType.DEDUCTION_PAYMENT;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();

		List<RecordResult> recordResults = new ArrayList<>();

		/*
		 * STEP 1: READ EXCEL
		 */
		List<DeductionPaymentRecord> records = excelReader.read(request.getFilePath());

		/*
		 * STEP 2: GET MIGRATION JOB
		 */
		MigrationJob job = migrationJobRepository.findByJobId(request.getJobId())
				.orElseThrow(() -> new IllegalArgumentException("Migration job not found: " + request.getJobId()));

		/*
		 * STEP 3: INITIALIZE JOB PROGRESS
		 */
		job.setTotalRecords(records.size());
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");
		job.setCurrentMessage("Excel read successfully. Starting deduction payment migration...");

		migrationJobRepository.saveAndFlush(job);
		progressPublisher.publish(job);

		int success = 0;
		int failed = 0;
		int skipped = 0;
		int processed = 0;

		/*
		 * STEP 4: PROCESS EACH DEDUCTION PAYMENT
		 *
		 * One Excel row = one API request.
		 */
		for (int i = 0; i < records.size(); i++) {

			if (cancellationManager.isCancelled(request.getJobId())) {

				job.setStatus("CANCELLED");
				job.setCurrentMessage("Migration cancelled by user.");
				job.setCompletedTime(LocalDateTime.now());

				migrationJobRepository.saveAndFlush(job);
				progressPublisher.publish(job);

				break;
			}

			DeductionPaymentRecord record = records.get(i);

			long recordStart = System.currentTimeMillis();

			RecordResult result = new RecordResult();
			result.setRecordNumber(i + 1);
			result.setStartRow(record.getStartRow());
			result.setEndRow(record.getEndRow());

			List<String> recordKeys;

			/*
			 * Generate duplicate-detection keys.
			 *
			 * If key generation fails, record this row as failed and continue.
			 */
			try {
				recordKeys = getRecordKeys(record);
			} catch (Exception e) {

				result.setStatus(RecordStatus.FAILED);
				result.setMessage(getRootCauseMessage(e));
				result.setExecutionTime(System.currentTimeMillis() - recordStart);

				failed++;
				processed++;
				recordResults.add(result);

				saveMigrationDetail(job, request, result, RecordStatus.FAILED.name(), new ArrayList<String>());

				updateJobProgress(job, processed, records.size(), success, failed, skipped,
						"Deduction payment row " + record.getStartRow() + " failed during validation.");

				continue;
			}

			/*
			 * DUPLICATE CHECK
			 */
			try {

				boolean alreadyMigrated = duplicateDetectionService.isAlreadyMigrated(request.getTenantId(),
						request.getMigrationType().name(), recordKeys);

				if (alreadyMigrated) {

					result.setStatus(RecordStatus.SKIPPED);
					result.setMessage("Deduction payment already migrated.");
					result.setExecutionTime(0L);

					skipped++;
					processed++;
					recordResults.add(result);

					saveMigrationDetail(job, request, result, RecordStatus.SKIPPED.name(), recordKeys);

					updateJobProgress(job, processed, records.size(), success, failed, skipped,
							"Deduction payment row " + record.getStartRow() + " skipped - already migrated.");

					continue;
				}

			} catch (Exception e) {

				/*
				 * Do not call the API if duplicate detection itself failed. Otherwise, a
				 * previously migrated payment might be created again.
				 */
				result.setStatus(RecordStatus.FAILED);
				result.setMessage("Duplicate check failed: " + getRootCauseMessage(e));
				result.setExecutionTime(System.currentTimeMillis() - recordStart);

				failed++;
				processed++;
				recordResults.add(result);

				saveMigrationDetail(job, request, result, RecordStatus.FAILED.name(), recordKeys);

				updateJobProgress(job, processed, records.size(), success, failed, skipped,
						"Deduction payment row " + record.getStartRow() + " failed during duplicate detection.");

				continue;
			}

			/*
			 * CREATE DEDUCTION PAYMENT
			 */
			try {

				CreateDeductionPaymentRequest deductionRequest = requestBuilder.build(record, request);

				if (deductionRequest == null) {
					throw new IllegalStateException("Unable to build deduction payment API request.");
				}

				DeductionPaymentResponse response = deductionPaymentApiClient.create(deductionRequest);

				if (response == null) {
					throw new IllegalStateException("Deduction payment API returned a null response.");
				}

				if (!response.isSuccess()) {
					String message = response.getMessage();

					if (message == null || message.trim().isEmpty()) {
						message = "Deduction payment API returned an unsuccessful response.";
					}

					throw new IllegalStateException(message);
				}

				result.setStatus(RecordStatus.SUCCESS);
				result.setMessage(response.getMessage() != null ? response.getMessage()
						: "Deduction payment created successfully.");

				success++;

			} catch (Exception e) {

				result.setStatus(RecordStatus.FAILED);
				result.setMessage(getRootCauseMessage(e));
				failed++;

			} finally {

				result.setExecutionTime(System.currentTimeMillis() - recordStart);
			}

			/*
			 * SAVE ROW RESULT
			 */
			processed++;
			recordResults.add(result);

			saveMigrationDetail(job, request, result, result.getStatus().name(), recordKeys);

			/*
			 * UPDATE REAL-TIME PROGRESS
			 */
			updateJobProgress(job, processed, records.size(), success, failed, skipped,
					"Processing deduction payment " + processed + " of " + records.size());
		}

		/*
		 * STEP 5: FINALIZE CANCELLED JOB
		 */
		if ("CANCELLED".equals(job.getStatus())) {

			job.setTotalRecords(records.size());
			job.setSuccessRecords(success);
			job.setFailedRecords(failed);
			job.setSkippedRecords(skipped);
			job.setCompletedTime(LocalDateTime.now());

			migrationJobRepository.saveAndFlush(job);
			progressPublisher.publish(job);

			cancellationManager.remove(request.getJobId());

			return MigrationResult.builder().success(false).message("Migration cancelled by user.")
					.totalRecords(records.size()).successRecords(success).failedRecords(failed).skippedRecords(skipped)
					.recordResults(recordResults).totalExecutionTime(System.currentTimeMillis() - startTime).build();
		}

		/*
		 * STEP 6: FINAL JOB STATUS
		 */
		job.setTotalRecords(records.size());
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setCurrentRecord(processed);

		job.setProgressPercent(records.isEmpty() ? 100 : (int) (((double) processed / records.size()) * 100));

		String finalMessage;

		if (failed > 0) {
			finalMessage = "Deduction payment migration completed with " + failed + " failed record(s).";
		} else if (skipped > 0) {
			finalMessage = "Deduction payment migration completed successfully. " + skipped
					+ " record(s) skipped as duplicates.";
		} else {
			finalMessage = "Deduction payment migration completed successfully.";
		}

		job.setCurrentMessage(finalMessage);
		job.setStatus(failed > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
		job.setCompletedTime(LocalDateTime.now());

		migrationJobRepository.saveAndFlush(job);
		progressPublisher.publish(job);

		cancellationManager.remove(request.getJobId());

		/*
		 * STEP 7: RETURN RESULT
		 */
		return MigrationResult.builder().success(failed == 0).message(finalMessage).totalRecords(records.size())
				.successRecords(success).failedRecords(failed).skippedRecords(skipped).recordResults(recordResults)
				.totalExecutionTime(System.currentTimeMillis() - startTime).build();
	}

	/**
	 * Update real-time migration progress.
	 */
	private void updateJobProgress(MigrationJob job, int currentRecord, int totalRecords, int success, int failed,
			int skipped, String message) {

		job.setCurrentRecord(currentRecord);
		job.setTotalRecords(totalRecords);

		int progress = totalRecords == 0 ? 100 : (int) (((double) currentRecord / totalRecords) * 100);

		job.setProgressPercent(progress);
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setCurrentMessage(message);

		migrationJobRepository.saveAndFlush(job);
		progressPublisher.publish(job);
	}

	/**
	 * Persist row-level migration details.
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
	 * Return the root cause message.
	 */
	private String getRootCauseMessage(Throwable exception) {

		Throwable root = exception;

		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}

		if (root.getMessage() == null || root.getMessage().trim().isEmpty()) {
			return root.getClass().getSimpleName();
		}

		return root.getMessage();
	}

	/**
	 * Generate stable keys for duplicate detection.
	 *
	 * A payment is identified using its source voucher details and recovery, fund
	 * and amount.
	 */
	@Override
	protected List<String> getRecordKeys(Object record) {

		if (!(record instanceof DeductionPaymentRecord)) {
			throw new IllegalArgumentException("Invalid record type for DeductionPaymentMigrationProcessor.");
		}

		DeductionPaymentRecord payment = (DeductionPaymentRecord) record;

		List<String> keys = new ArrayList<String>();

		String voucherNumber = normalize(payment.getVoucherNumber());
		String voucherType = normalize(payment.getVoucherType());
		String recovery = normalize(payment.getRecovery());
		String fund = normalize(payment.getFund());

		if (voucherNumber.isEmpty()) {
			throw new IllegalArgumentException("voucher number is missing.");
		}

		if (voucherType.isEmpty()) {
			throw new IllegalArgumentException("voucher type is missing.");
		}

		if (recovery.isEmpty()) {
			throw new IllegalArgumentException("Recovery name is missing.");
		}

		if (fund.isEmpty()) {
			throw new IllegalArgumentException("Fund name is missing.");
		}

		if (payment.getVoucherDate() == null) {
			throw new IllegalArgumentException("voucher date is missing.");
		}

		if (payment.getDeductionAmount() == null) {
			throw new IllegalArgumentException("Deduction amount is missing.");
		}

		String voucherDate = String.valueOf(payment.getVoucherDate().getTime());
		String amount = payment.getDeductionAmount().stripTrailingZeros().toPlainString();
		String key = "DEDUCTION_PAYMENT:" + voucherNumber + "|" + voucherType + "|" + voucherDate + "|" + recovery + "|"
				+ fund + "|" + amount;

		keys.add(key);

		return keys;
	}

	protected String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}