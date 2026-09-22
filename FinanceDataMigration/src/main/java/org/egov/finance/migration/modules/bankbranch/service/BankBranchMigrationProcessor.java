package org.egov.finance.migration.modules.bankbranch.service;

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
import org.egov.finance.migration.modules.bankbranch.dto.BankBranchRecord;
import org.egov.finance.migration.modules.bankbranch.dto.CreateBankBranchRequest;
import org.egov.finance.migration.modules.bankbranch.reader.BankBranchExcelReader;
import org.egov.finance.migration.modules.bankbranch.response.BankBranchResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;
import org.springframework.stereotype.Service;

@Service
public class BankBranchMigrationProcessor
		extends AbstractMigrationProcessor {

	private final BankBranchExcelReader excelReader;
	private final BankBranchRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final BankBranchApiClient bankBranchApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public BankBranchMigrationProcessor(
			BankBranchExcelReader excelReader,
			BankBranchRequestBuilder requestBuilder,
			DuplicateDetectionService duplicateDetectionService,
			BankBranchApiClient bankBranchApiClient,
			MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository,
			MigrationCancellationManager cancellationManager,
			MigrationProgressPublisher progressPublisher) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.bankBranchApiClient = bankBranchApiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
        this.cancellationManager = cancellationManager;
        this.progressPublisher = progressPublisher;
	}

	@Override
	public MigrationType getMigrationType() {

		return MigrationType.BANK_BRANCH;
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

		List<BankBranchRecord> records =
				excelReader.read(request.getFilePath());

		/*
		 * ============================================================
		 * STEP 2 : GET EXISTING MIGRATION JOB
		 * ============================================================
		 */

		MigrationJob job = migrationJobRepository
				.findByJobId(request.getJobId())
				.orElseThrow(() -> new IllegalArgumentException(
						"Migration job not found: "
								+ request.getJobId()));

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
				"Excel read successfully. Starting bank branch migration...");

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
		 * STEP 4 : PROCESS EACH BANK BRANCH INDEPENDENTLY
		 *
		 * ONE BANK BRANCH = ONE API REQUEST
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

			BankBranchRecord record = records.get(i);

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
			
			String recordKey = getRecordKey(record);

			boolean alreadyMigrated =
					duplicateDetectionService.isAlreadyMigrated(
							request.getTenantId(),
							request.getMigrationType().name(),
							recordKey);

			if (alreadyMigrated) {

				result.setStatus(RecordStatus.SKIPPED);
				result.setMessage(
						"Bank Branch already migrated.");
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
						RecordStatus.SKIPPED.name(),
						recordKey);

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
						"Bank Branch " + (i + 1)
								+ " of "
								+ records.size()
								+ " skipped - already migrated.");

				continue;
			}

			/*
			 * ========================================================
			 * PROCESS CURRENT BANK BRANCH
			 *
			 * ONE BANK BRANCH = ONE API REQUEST
			 * ========================================================
			 */

			try {

				CreateBankBranchRequest bankBranchRequest =
						requestBuilder.build(record, request);

				if (bankBranchRequest == null) {

					throw new RuntimeException(
							"Unable to build BankBranchRequest.");
				}

				/*
				 * Create Bank Branch through Finance API
				 */
				BankBranchResponse response =
						bankBranchApiClient
								.createBankBranch(bankBranchRequest);

				if (response == null) {

					System.out.println(
							"Bank Branch API returned null response");

					result.setStatus(RecordStatus.FAILED);
					result.setMessage(
							"Bank Branch API returned null response");

					failed++;

				} else {

					result.setStatus(RecordStatus.SUCCESS);
					result.setMessage(
							"Bank Branch created successfully.");

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
					result.getStatus().name(),
					recordKey);

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
					"Processing bank branch "
							+ (i + 1)
							+ " of "
							+ records.size());
		}

		/*
		 * ============================================================
		 * STEP 5 : FINAL JOB STATUS
		 * ============================================================
		 */

        if ("CANCELLED".equals(job.getStatus())) {

            job.setCompletedTime(LocalDateTime.now());
            migrationJobRepository.save(job);

            cancellationManager.remove(request.getJobId());

            return MigrationResult.builder()
                    .success(false)
                    .message("Migration cancelled by user.")
                    .totalRecords(records.size())
                    .successRecords(success)
                    .failedRecords(failed)
                    .skippedRecords(skipped)
                    .recordResults(recordResults)
                    .totalExecutionTime(
                            System.currentTimeMillis() - startTime)
                    .build();
        }
        
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
					"Bank Branch migration completed with "
							+ failed
							+ " failed record(s).";

		} else if (skipped > 0) {

			finalMessage =
					"Bank Branch migration completed successfully. "
							+ skipped
							+ " record(s) skipped as duplicate.";

		} else {

			finalMessage =
					"Bank Branch migration completed successfully.";
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
		progressPublisher.publish(job);

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
		progressPublisher.publish(job);
	}

	/**
	 * Save migration detail record.
	 */
	private void saveMigrationDetail(
			MigrationJob job,
			MigrationRequest request,
			RecordResult result,
			String status,
			String recordKey) {

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

		detail.setRecordKey(recordKey);

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
	
	@Override
	protected String getRecordKey(Object record) {

	    if (!(record instanceof BankBranchRecord branch)) {
	        throw new IllegalArgumentException(
	                "Invalid record type for BankBranchMigrationProcessor");
	    }

	    String bankName = normalize(branch.getBankName());
	    String branchName = normalize(branch.getBranchName());
	    String ifscCode = normalize(branch.getIfscCode());
	    String branchCode = normalize(branch.getBranchCode());

	    if (!bankName.isEmpty()
	            && !branchName.isEmpty()
	            && !ifscCode.isEmpty()
	            && !branchCode.isEmpty()) {

	        return "BANK:" + bankName
	                + "|BRANCH:" + branchName
	                + "|IFSC:" + ifscCode
	                + "|BRANCH_CODE:" + branchCode;
	    }

	    throw new IllegalArgumentException(
	            "Unable to generate unique record key for bank branch. "
	            + "Bank name, branch name, IFSC code and branch code "
	            + "are required.");
	}
}