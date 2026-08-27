package org.egov.finance.migration.modules.bank.service;

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
import org.egov.finance.migration.modules.bank.dto.BankRecord;
import org.egov.finance.migration.modules.bank.dto.CreateBankRequest;
import org.egov.finance.migration.modules.bank.reader.BankExcelReader;
import org.egov.finance.migration.modules.bank.response.BankResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.springframework.stereotype.Service;

@Service
public class BankMigrationProcessor extends AbstractMigrationProcessor {

	private final BankExcelReader excelReader;
	private final BankRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final BankApiClient bankApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;

	public BankMigrationProcessor(
			BankExcelReader excelReader,
			BankRequestBuilder requestBuilder,
			DuplicateDetectionService duplicateDetectionService,
			BankApiClient bankApiClient,
			MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.bankApiClient = bankApiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
	}

	@Override
	public MigrationType getMigrationType() {

		return MigrationType.BANK;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();

		List<RecordResult> recordResults = new ArrayList<>();

		/*
		 * ============================================================
		 * STEP 1 : READ EXCEL
		 * ============================================================
		 */

		List<BankRecord> records = excelReader.read(request.getFile());

		/*
		 * ============================================================
		 * STEP 2 : GET EXISTING MIGRATION JOB
		 * ============================================================
		 */

		MigrationJob job = migrationJobRepository
				.findByJobId(request.getJobId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Migration job not found: " + request.getJobId()));

		/*
		 * ============================================================
		 * STEP 3 : INITIALIZE JOB PROGRESS
		 * ============================================================
		 */

		job.setTotalRecords(records.size());
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");
		job.setCurrentMessage(
				"Excel read successfully. Starting bank migration...");

		migrationJobRepository.save(job);

		/*
		 * ============================================================
		 * COUNTERS
		 * ============================================================
		 */

		int success = 0;
		int failed = 0;
		int skipped = 0;

		/*
		 * ============================================================
		 * STEP 4 : PROCESS EACH BANK INDEPENDENTLY
		 *
		 * ONE BANK = ONE API REQUEST
		 * ============================================================
		 */

		for (int i = 0; i < records.size(); i++) {

			BankRecord record = records.get(i);

			long recordStart = System.currentTimeMillis();

			RecordResult result = new RecordResult();

			result.setRecordNumber(i + 1);
			result.setStartRow(record.getStartRow());
			result.setEndRow(record.getEndRow());

			/*
			 * ========================================================
			 * DUPLICATE CHECK
			 * ========================================================
			 */

			boolean alreadyMigrated =
					duplicateDetectionService.isAlreadyMigrated(
							request.getTenantId(),
							request.getMigrationType().name(),
							record.getStartRow(),
							record.getEndRow());

			if (alreadyMigrated) {

				result.setStatus(RecordStatus.SKIPPED);
				result.setMessage("Bank already migrated.");
				result.setExecutionTime(0L);

				skipped++;

				recordResults.add(result);

				/*
				 * Save skipped record
				 */
				saveMigrationDetail(
						job,
						request,
						result,
						RecordStatus.SKIPPED.name());

				/*
				 * Update progress
				 */
				updateJobProgress(
						job,
						i + 1,
						records.size(),
						success,
						failed,
						skipped,
						"Bank " + (i + 1)
								+ " of "
								+ records.size()
								+ " skipped - already migrated.");

				continue;
			}

			/*
			 * ========================================================
			 * PROCESS CURRENT BANK
			 *
			 * ONE BANK = ONE API REQUEST
			 * ========================================================
			 */

			try {

				CreateBankRequest bankRequest =
						requestBuilder.build(record, request);

				if (bankRequest == null) {

					throw new RuntimeException(
							"Unable to build BankRequest.");
				}

				/*
				 * Create Bank through Finance API
				 */
				BankResponse response =
						bankApiClient.createBank(bankRequest);

				if (response == null) {

					System.out.println(
							"Bank API returned null response");

					result.setStatus(RecordStatus.FAILED);
					result.setMessage(
							"Bank API returned null response");

					failed++;

				} else {

					result.setStatus(RecordStatus.SUCCESS);
					result.setMessage(
							"Bank created successfully.");

					success++;
				}

			} catch (Exception e) {

				result.setStatus(RecordStatus.FAILED);

				String errorMessage =
						getRootCauseMessage(e);

				result.setMessage(errorMessage);

				failed++;

			} finally {

				result.setExecutionTime(
						System.currentTimeMillis()
								- recordStart);
			}

			/*
			 * ========================================================
			 * SAVE RESULT
			 * ========================================================
			 */

			recordResults.add(result);

			saveMigrationDetail(
					job,
					request,
					result,
					result.getStatus().name());

			/*
			 * ========================================================
			 * UPDATE REALTIME PROGRESS
			 * ========================================================
			 */

			updateJobProgress(
					job,
					i + 1,
					records.size(),
					success,
					failed,
					skipped,
					"Processing bank "
							+ (i + 1)
							+ " of "
							+ records.size());
		}

		/*
		 * ============================================================
		 * STEP 5 : FINAL JOB STATUS
		 * ============================================================
		 */

		job.setTotalRecords(records.size());
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setProgressPercent(100);
		job.setCurrentRecord(records.size());

		/*
		 * ============================================================
		 * FINAL MESSAGE
		 * ============================================================
		 */

		String finalMessage;

		if (failed > 0) {

			finalMessage =
					"Bank migration completed with "
							+ failed
							+ " failed record(s).";

		} else if (skipped > 0) {

			finalMessage =
					"Bank migration completed successfully. "
							+ skipped
							+ " record(s) skipped as duplicate.";

		} else {

			finalMessage =
					"Bank migration completed successfully.";
		}

		job.setCurrentMessage(finalMessage);

		/*
		 * ============================================================
		 * FINAL STATUS
		 * ============================================================
		 */

		if (failed > 0) {

			job.setStatus("COMPLETED_WITH_ERRORS");

		} else {

			job.setStatus("COMPLETED");
		}

		job.setCompletedTime(LocalDateTime.now());

		migrationJobRepository.save(job);

		/*
		 * ============================================================
		 * FINAL EXECUTION TIME
		 * ============================================================
		 */

		long totalExecutionTime =
				System.currentTimeMillis() - startTime;

		/*
		 * ============================================================
		 * RETURN FINAL RESULT
		 * ============================================================
		 */

		return MigrationResult.builder()
				.success(failed == 0)
				.message(finalMessage)
				.totalRecords(records.size())
				.successRecords(success)
				.failedRecords(failed)
				.skippedRecords(skipped)
				.recordResults(recordResults)
				.totalExecutionTime(totalExecutionTime)
				.build();
	}

	/**
	 * Update realtime migration job progress.
	 */
	private void updateJobProgress(
			MigrationJob job,
			int currentRecord,
			int totalRecords,
			int success,
			int failed,
			int skipped,
			String message) {

		job.setCurrentRecord(currentRecord);
		job.setTotalRecords(totalRecords);

		int progress = 0;

		if (totalRecords > 0) {

			progress = (int) (((double) currentRecord
					/ totalRecords) * 100);
		}

		job.setProgressPercent(progress);
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setCurrentMessage(message);

		migrationJobRepository.saveAndFlush(job);
	}

	/**
	 * Save migration detail record.
	 */
	private void saveMigrationDetail(
			MigrationJob job,
			MigrationRequest request,
			RecordResult result,
			String status) {

		MigrationJobDetail detail =
				new MigrationJobDetail();

		detail.setJob(job);
		detail.setTenantId(request.getTenantId());
		detail.setModuleCode(
				request.getMigrationType().name());
		detail.setRecordNumber(
				result.getRecordNumber());
		detail.setStartRow(
				result.getStartRow());
		detail.setEndRow(
				result.getEndRow());
		detail.setStatus(status);
		detail.setMessage(
				result.getMessage());
		detail.setExecutionTime(
				result.getExecutionTime());

		detail.setRecordKey(
				request.getMigrationType().name()
						+ ":"
						+ result.getStartRow()
						+ "-"
						+ result.getEndRow());

		detail.setCreatedTime(
				LocalDateTime.now());

		migrationJobDetailRepository.save(detail);
	}

	/**
	 * Get actual root cause message.
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
}