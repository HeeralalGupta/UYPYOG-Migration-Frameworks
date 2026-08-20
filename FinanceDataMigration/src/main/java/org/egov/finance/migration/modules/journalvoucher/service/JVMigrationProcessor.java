package org.egov.finance.migration.modules.journalvoucher.service;

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
import org.egov.finance.migration.modules.journalvoucher.dto.JournalVoucherRecord;
import org.egov.finance.migration.modules.journalvoucher.dto.VoucherRequest;
import org.egov.finance.migration.modules.journalvoucher.reader.JournalVoucherExcelReader;
import org.egov.finance.migration.modules.journalvoucher.response.VoucherResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.springframework.stereotype.Service;

@Service
public class JVMigrationProcessor extends AbstractMigrationProcessor {

	private final JournalVoucherExcelReader excelReader;
	private final JournalVoucherRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final JournalVoucherApiClient journalVoucherApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;

	public JVMigrationProcessor(JournalVoucherExcelReader excelReader, JournalVoucherRequestBuilder requestBuilder,
			DuplicateDetectionService duplicateDetectionService, JournalVoucherApiClient journalVoucherApiClient,
			MigrationJobRepository migrationJobRepository, MigrationJobDetailRepository migrationJobDetailRepository) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.journalVoucherApiClient = journalVoucherApiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
	}

	@Override
	public MigrationType getMigrationType() {

		return MigrationType.JOURNAL_VOUCHER;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();

		List<RecordResult> recordResults = new ArrayList<>();

		/*
		 * ============================================================ STEP 1 : READ
		 * EXCEL ============================================================
		 */

		List<JournalVoucherRecord> records = excelReader.read(request.getFile());

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
		job.setCurrentMessage("Excel read successfully. Starting migration...");

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
		 * EACH VOUCHER INDEPENDENTLY
		 *
		 * IMPORTANT:
		 *
		 * Each loop iteration represents ONE voucher.
		 *
		 * If Voucher 3 fails:
		 *
		 * Voucher 1 -> SUCCESS Voucher 2 -> SUCCESS Voucher 3 -> FAILED Voucher 4 ->
		 * STILL PROCESSED Voucher 5 -> STILL PROCESSED
		 *
		 * One failure will NOT stop the loop.
		 * ============================================================
		 */

		for (int i = 0; i < records.size(); i++) {

			JournalVoucherRecord record = records.get(i);

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
				result.setMessage("Voucher already migrated.");
				result.setExecutionTime(0L);
				skipped++;

				recordResults.add(result);

				/*
				 * Save skipped record in detail table
				 */
				saveMigrationDetail(job, request, result, RecordStatus.SKIPPED.name());

				/*
				 * Update progress
				 */
				updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
						"Voucher " + (i + 1) + " of " + records.size() + " skipped - already migrated.");

				/*
				 * VERY IMPORTANT
				 *
				 * Do not process duplicate voucher. Move to next voucher.
				 */

				continue;
			}

			/*
			 * ======================================================== PROCESS CURRENT
			 * VOUCHER
			 *
			 * ONE VOUCHER = ONE API REQUEST
			 * ========================================================
			 */

			try {

				VoucherRequest voucherRequest = requestBuilder.build(record, request);

				if (voucherRequest == null || voucherRequest.getVouchers() == null
						|| voucherRequest.getVouchers().isEmpty()) {

					throw new RuntimeException("Unable to build VoucherRequest.");
				}

				VoucherResponse response = journalVoucherApiClient.createVoucher(voucherRequest);
				result.setStatus(RecordStatus.SUCCESS);
				result.setMessage("Voucher created successfully.");
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
			 * Save result exactly once
			 */
			recordResults.add(result);
			saveMigrationDetail(job, request, result, result.getStatus().name());

			/*
			 * Update realtime progress
			 */
			updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
					"Processing voucher " + (i + 1) + " of " + records.size());
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

		/*
		 * ============================================================ FINAL MESSAGE
		 * ============================================================
		 */

		String finalMessage;

		if (failed > 0) {

			finalMessage = "Migration completed with " + failed + " failed record(s).";
		} else if (skipped > 0) {

			finalMessage = "Migration completed successfully. " + skipped + " record(s) skipped as duplicate.";
		} else {

			finalMessage = "Migration completed successfully.";
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

		/*
		 * ============================================================ FINAL EXECUTION
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