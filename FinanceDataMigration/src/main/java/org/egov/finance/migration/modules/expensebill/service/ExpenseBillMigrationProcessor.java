package org.egov.finance.migration.modules.expensebill.service;

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
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillCreateRequest;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRecord;
import org.egov.finance.migration.modules.expensebill.reader.ExpenseBillExcelReader;
import org.egov.finance.migration.modules.expensebill.response.ExpenseBillResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.springframework.stereotype.Service;

@Service
public class ExpenseBillMigrationProcessor extends AbstractMigrationProcessor {

	private final ExpenseBillExcelReader excelReader;

	private final ExpenseBillRequestBuilder requestBuilder;

	private final DuplicateDetectionService duplicateDetectionService;

	private final ExpenseBillApiClient expenseBillApiClient;

	private final MigrationJobRepository migrationJobRepository;

	private final MigrationJobDetailRepository migrationJobDetailRepository;

	public ExpenseBillMigrationProcessor(ExpenseBillExcelReader excelReader, ExpenseBillRequestBuilder requestBuilder,
			DuplicateDetectionService duplicateDetectionService, ExpenseBillApiClient expenseBillApiClient,
			MigrationJobRepository migrationJobRepository, MigrationJobDetailRepository migrationJobDetailRepository) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.expenseBillApiClient = expenseBillApiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
	}

	@Override
	public MigrationType getMigrationType() {
		return MigrationType.EXPENSE_BILL;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();

		List<RecordResult> recordResults = new ArrayList<>();

		/*
		 * ============================================================ STEP 1 : READ
		 * EXCEL ============================================================
		 */

		List<ExpenseBillRecord> records = new ArrayList<>();
		try {
			records = excelReader.read(request.getFile());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * ============================================================ STEP 2 : GET
		 * MIGRATION JOB ============================================================
		 */

		MigrationJob job = migrationJobRepository.findByJobId(request.getJobId())
				.orElseThrow(() -> new IllegalArgumentException("Migration job not found: " + request.getJobId()));

		/*
		 * ============================================================ STEP 3 :
		 * INITIALIZE JOB ============================================================
		 */

		job.setTotalRecords(records.size());
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");

		job.setCurrentMessage("Expense bill Excel read successfully. Starting migration...");

		migrationJobRepository.save(job);

		int success = 0;
		int failed = 0;
		int skipped = 0;

		/*
		 * ============================================================ STEP 4 : PROCESS
		 * EACH EXPENSE BILL
		 *
		 * ONE ExpenseBillRecord = ONE API REQUEST
		 * ============================================================
		 */

		for (int i = 0; i < records.size(); i++) {

			ExpenseBillRecord record = records.get(i);

			long recordStart = System.currentTimeMillis();

			RecordResult result = new RecordResult();

			result.setRecordNumber(i + 1);
			result.setStartRow(record.getStartRow());
			result.setEndRow(record.getEndRow());

			/*
			 * ======================================================== DUPLICATE CHECK
			 * ========================================================
			 */

			boolean alreadyMigrated = duplicateDetectionService.isAlreadyMigrated(request.getTenantId(),
					request.getMigrationType().name(), record.getStartRow(), record.getEndRow());

			if (alreadyMigrated) {

				result.setStatus(RecordStatus.SKIPPED);
				result.setMessage("Expense bill already migrated.");
				result.setExecutionTime(0L);

				skipped++;

				recordResults.add(result);

				saveMigrationDetail(job, request, result, RecordStatus.SKIPPED.name());

				updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
						"Expense bill " + (i + 1) + " of " + records.size() + " skipped - already migrated.");

				continue;
			}

			/*
			 * ======================================================== BUILD REQUEST + CALL
			 * EXPENSE BILL API ========================================================
			 */

			try {

				/*
				 * Builder responsibilities:
				 *
				 * 1. Resolve GL Code -> GL ID 2. Resolve Account Detail Type -> ID 3. Resolve
				 * Account Detail Key -> ID 4. Build billDetails 5. Build billPayeedetails 6.
				 * Build checklist 7. Build MIS details
				 */

				ExpenseBillCreateRequest expenseBillRequest = requestBuilder.build(record, request);

				if (expenseBillRequest == null || expenseBillRequest.getExpenseBillRequest() == null
						|| expenseBillRequest.getExpenseBillRequest().getEgBillregister() == null) {

					throw new RuntimeException("Unable to build ExpenseBillRequest.");
				}

				/*
				 * Call Expense Bill Creation API
				 */

				ExpenseBillResponse response = expenseBillApiClient.createExpenseBill(expenseBillRequest);

				/*
				 * Optional response validation
				 */

				if (response == null) {

					throw new RuntimeException("Expense Bill API returned empty response.");
				}

				result.setStatus(RecordStatus.SUCCESS);

				result.setMessage("Expense bill created successfully.");

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

			saveMigrationDetail(job, request, result, result.getStatus().name());

			/*
			 * ======================================================== UPDATE REALTIME
			 * PROGRESS ========================================================
			 */

			updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
					"Processing Expense Bill " + (i + 1) + " of " + records.size());
		}

		/*
		 * ============================================================ STEP 5 : FINAL
		 * JOB STATUS ============================================================
		 */

		job.setTotalRecords(records.size());

		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);

		job.setProgressPercent(100);

		job.setCurrentRecord(records.size());

		String finalMessage;

		if (failed > 0) {

			finalMessage = "Expense Bill migration completed with " + failed + " failed record(s).";

		} else if (skipped > 0) {

			finalMessage = "Expense Bill migration completed successfully. " + skipped
					+ " record(s) skipped as duplicate.";

		} else {

			finalMessage = "Expense Bill migration completed successfully.";
		}

		job.setCurrentMessage(finalMessage);

		if (failed > 0) {

			job.setStatus("COMPLETED_WITH_ERRORS");

		} else {

			job.setStatus("COMPLETED");
		}

		job.setCompletedTime(LocalDateTime.now());

		migrationJobRepository.save(job);

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

	/*
	 * ============================================================ UPDATE JOB
	 * PROGRESS ============================================================
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
	}

	/*
	 * ============================================================ SAVE MIGRATION
	 * DETAIL ============================================================
	 */

	private void saveMigrationDetail(MigrationJob job, MigrationRequest request, RecordResult result, String status) {

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

		detail.setRecordKey(request.getMigrationType().name() + ":" + result.getStartRow() + "-" + result.getEndRow());

		detail.setCreatedTime(LocalDateTime.now());

		migrationJobDetailRepository.save(detail);
	}

	/*
	 * ============================================================ GET ROOT CAUSE
	 * ============================================================
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