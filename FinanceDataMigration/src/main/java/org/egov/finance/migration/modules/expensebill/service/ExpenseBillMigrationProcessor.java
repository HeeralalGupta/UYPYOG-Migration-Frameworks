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
import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;
import org.springframework.stereotype.Service;

@Service
public class ExpenseBillMigrationProcessor extends AbstractMigrationProcessor {

	private final ExpenseBillExcelReader excelReader;
	private final ExpenseBillRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final ExpenseBillApiClient expenseBillApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public ExpenseBillMigrationProcessor(ExpenseBillExcelReader excelReader, ExpenseBillRequestBuilder requestBuilder,
			DuplicateDetectionService duplicateDetectionService, ExpenseBillApiClient expenseBillApiClient,
			MigrationJobRepository migrationJobRepository, MigrationJobDetailRepository migrationJobDetailRepository, MigrationCancellationManager cancellationManager, MigrationProgressPublisher progressPublisher) {

		this.excelReader = requireObject(excelReader, "ExpenseBillExcelReader");
		this.requestBuilder = requireObject(requestBuilder, "ExpenseBillRequestBuilder");
		this.duplicateDetectionService = requireObject(duplicateDetectionService, "DuplicateDetectionService");
		this.expenseBillApiClient = requireObject(expenseBillApiClient, "ExpenseBillApiClient");
		this.migrationJobRepository = requireObject(migrationJobRepository, "MigrationJobRepository");
		this.migrationJobDetailRepository = requireObject(migrationJobDetailRepository, "MigrationJobDetailRepository");
        this.cancellationManager = cancellationManager;
        this.progressPublisher = progressPublisher;
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
		 * ============================================================ STEP 0 : VALIDATE MIGRATION REQUEST
		 */

		validateMigrationRequest(request);

		/*
		 * ============================================================ STEP 1 : READ EXCEL ============================================================
		 */

		List<ExpenseBillRecord> records = readExcel(request);

		if (records.isEmpty()) {
			throw new IllegalArgumentException("Expense Bill migration failed: " + "No Expense Bill records found in Excel file.");
		}

		/*
		 * ============================================================ STEP 2 : GET MIGRATION JOB ============================================================
		 */

		MigrationJob job = getMigrationJob(request);

		/*
		 * ============================================================ STEP 3 : INITIALIZE JOB ============================================================
		 */

		initializeJob(job, records.size());

		int success = 0;
		int failed = 0;
		int skipped = 0;

		/*
		 * ============================================================ STEP 4 : PROCESS EACH EXPENSE BILL
		 */

		for (int i = 0; i < records.size(); i++) {
			
            if (cancellationManager.isCancelled(request.getJobId())) {

                job.setStatus("CANCELLED");
                job.setCurrentMessage("Migration cancelled by user.");

                migrationJobRepository.saveAndFlush(job);
                progressPublisher.publish(job);

                break;
            }

			ExpenseBillRecord record = records.get(i);
			long recordStart = System.currentTimeMillis();
			RecordResult result = new RecordResult();
			result.setRecordNumber(i + 1);

			if (record != null) {
				result.setStartRow(record.getStartRow());
				result.setEndRow(record.getEndRow());
			}
			
			 String recordKey = getRecordKey(record);

			try {

				/*
				 * ==================================================== 4.1 VALIDATE RECORD ====================================================
				 */

				validateRecord(record, i);

				/*
				 * ==================================================== 4.2 DUPLICATE CHECK ====================================================
				 */
				

				boolean alreadyMigrated = checkDuplicate(request, record);

				if (alreadyMigrated) {
					result.setStatus(RecordStatus.SKIPPED);
					result.setMessage("Expense bill already migrated for " + "Excel rows " + record.getStartRow() + "-"
							+ record.getEndRow() + ".");
					skipped++;
				} else {

					/*
					 * ================================================ 4.3 BUILD REQUEST ================================================
					 */

					ExpenseBillCreateRequest expenseBillRequest = buildRequest(record, request);

					/*
					 * ================================================ 4.4 VALIDATE GENERATED REQUEST ================================================
					 */

					validateBuiltRequest(expenseBillRequest, record, request);

					/*
					 * ================================================ 4.5 CALL API ================================================
					 */

					ExpenseBillResponse response = createExpenseBill(expenseBillRequest, record);

					/*
					 * ================================================ 4.6 VALIDATE API RESPONSE ================================================
					 */

					validateApiResponse(response, record);

					/*
					 * ================================================ SUCCESS ================================================
					 */

					result.setStatus(RecordStatus.SUCCESS);
					result.setMessage("Expense bill created successfully.");
					success++;
				}

			} catch (IllegalArgumentException e) {

				/*
				 * Validation/business exception.
				 *
				 * IMPORTANT: Preserve the detailed message generated by
				 * ExpenseBillRequestBuilder.
				 */

				result.setStatus(RecordStatus.FAILED);
				result.setMessage(buildRecordErrorMessage(record, e));
				failed++;

			} catch (Exception e) {

				/*
				 * Unexpected technical exception.
				 */
				result.setStatus(RecordStatus.FAILED);
				result.setMessage(buildUnexpectedErrorMessage(record, e));
				failed++;
			} finally {
				result.setExecutionTime(System.currentTimeMillis() - recordStart);
			}

			/*
			 * ======================================================== SAVE RECORD RESULT
			 * ========================================================
			 */

			recordResults.add(result);
			saveMigrationDetail(job, request, result, recordKey);

			/*
			 * ======================================================== UPDATE JOB PROGRESS
			 * ========================================================
			 */

			updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
					"Processing Expense Bill " + (i + 1) + " of " + records.size());
		}

		/*
		 * ============================================================ STEP 5 :
		 * VALIDATE FINAL COUNTS
		 * ============================================================
		 */

		validateFinalCounts(records.size(), success, failed, skipped);

		/*
		 * ============================================================ STEP 6 : FINAL
		 * JOB STATUS ============================================================
		 */

		String finalMessage = buildFinalMessage(success, failed, skipped);
		
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
		job.setCurrentMessage(finalMessage);

		if (failed > 0) {
			job.setStatus("COMPLETED_WITH_ERRORS");
		} else {
			job.setStatus("COMPLETED");
		}

		job.setCompletedTime(LocalDateTime.now());
		saveFinalJob(job);
		progressPublisher.publish(job);

		/*
		 * ============================================================ STEP 7 : RETURN
		 * FINAL RESULT ============================================================
		 */

		long totalExecutionTime = System.currentTimeMillis() - startTime;

		return MigrationResult.builder().success(failed == 0).message(finalMessage).totalRecords(records.size())
				.successRecords(success).failedRecords(failed).skippedRecords(skipped).recordResults(recordResults)
				.totalExecutionTime(totalExecutionTime).build();
	}

	/*
	 * ================================================================ VALIDATE
	 * MIGRATION REQUEST
	 * ================================================================
	 */

	private void validateMigrationRequest(MigrationRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("MigrationRequest cannot be null.");
		}

		if (!hasText(request.getTenantId())) {
			throw new IllegalArgumentException("Tenant ID is required for Expense Bill migration.");
		}

		if (request.getJobId() == null) {
			throw new IllegalArgumentException("Migration Job ID is required.");
		}

		if (request.getMigrationType() == null) {
			throw new IllegalArgumentException("Migration type is required.");
		}

		if (request.getMigrationType() != MigrationType.EXPENSE_BILL) {
			throw new IllegalArgumentException("Invalid migration type. Expected " + MigrationType.EXPENSE_BILL
					+ " but received " + request.getMigrationType() + ".");
		}

		if (request.getFile() == null) {
			throw new IllegalArgumentException("Expense Bill Excel file is required.");
		}
	}

	/*
	 * ================================================================ READ EXCEL
	 * ================================================================
	 */

	private List<ExpenseBillRecord> readExcel(MigrationRequest request) {

		try {

			List<ExpenseBillRecord> records = excelReader.read(request.getFilePath());

			if (records == null) {
				throw new IllegalArgumentException("ExpenseBillExcelReader returned null.");
			}

			return records;

		} catch (IllegalArgumentException e) {
			throw e;

		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to read Expense Bill Excel file: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ GET
	 * MIGRATION JOB
	 * ================================================================
	 */

	private MigrationJob getMigrationJob(MigrationRequest request) {

		try {

			MigrationJob job = migrationJobRepository.findByJobId(request.getJobId()).orElse(null);

			if (job == null) {
				throw new IllegalArgumentException("Migration job not found for Job ID: " + request.getJobId());
			}
			return job;

		} catch (IllegalArgumentException e) {
			throw e;

		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to fetch migration job " + request.getJobId() + ": " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ INITIALIZE
	 * JOB ================================================================
	 */

	private void initializeJob(MigrationJob job, int totalRecords) {

		if (job == null) {
			throw new IllegalArgumentException("Migration job cannot be null.");
		}

		if (totalRecords <= 0) {
			throw new IllegalArgumentException("Total Expense Bill records must be greater than zero.");
		}

		job.setTotalRecords(totalRecords);
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");
		job.setCurrentMessage("Expense Bill Excel read successfully. " + "Starting migration...");

		try {
			migrationJobRepository.save(job);

		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to initialize migration job " + requestJobId(job) + ": " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ VALIDATE
	 * RECORD ================================================================
	 */

	private void validateRecord(ExpenseBillRecord record, int index) {

		if (record == null) {
			throw new IllegalArgumentException("Expense Bill record at index " + index + " is null.");
		}

		if (record.getSerialNumber() == null) {
			throw new IllegalArgumentException("Serial Number is required.");
		}

		if (record.getSerialNumber() <= 0) {
			throw new IllegalArgumentException("Serial Number must be greater than zero. " + "Received: " + record.getSerialNumber());
		}

		if (record.getStartRow() <= 0) {
			throw new IllegalArgumentException("Invalid Excel start row: " + record.getStartRow());
		}

		if (record.getEndRow() <= 0) {
			throw new IllegalArgumentException("Invalid Excel end row: " + record.getEndRow());
		}

		if (record.getEndRow() < record.getStartRow()) {
			throw new IllegalArgumentException("Invalid Excel row range. " + "Start row=" + record.getStartRow()
					+ ", End row=" + record.getEndRow() + ".");
		}
	}

	/*
	 * ================================================================ DUPLICATE
	 * CHECK ================================================================
	 */

	private boolean checkDuplicate(MigrationRequest request, ExpenseBillRecord record) {
		
		 String recordKey = getRecordKey(record);

		try {
			return duplicateDetectionService.isAlreadyMigrated(request.getTenantId(), request.getMigrationType().name(), recordKey);

		} catch (Exception e) {
			throw new IllegalArgumentException("Duplicate check failed for Expense Bill SN " + record.getSerialNumber()
					+ " [rows " + record.getStartRow() + "-" + record.getEndRow() + "]: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ BUILD
	 * REQUEST ================================================================
	 */

	private ExpenseBillCreateRequest buildRequest(ExpenseBillRecord record, MigrationRequest request) {

		try {
			ExpenseBillCreateRequest expenseBillRequest = requestBuilder.build(record, request);
			if (expenseBillRequest == null) {
				throw new IllegalArgumentException("ExpenseBillRequestBuilder returned null.");
			}
			return expenseBillRequest;
		} catch (IllegalArgumentException e) {

			/*
			 * DO NOT replace this exception.
			 *
			 * ExpenseBillRequestBuilder has the detailed field validation and should throw
			 * messages such as:
			 *
			 * Fund not found: General Fund Invalid GL Code: 3501000003 Net Payable amount
			 * mismatch...
			 */

			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unexpected error while building " + "Expense Bill request: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * =========================================== VALIDATE GENERATED REQUEST ======================================
	 */

	private void validateBuiltRequest(ExpenseBillCreateRequest expenseBillRequest, ExpenseBillRecord record,
			MigrationRequest migrationRequest) {

		if (expenseBillRequest == null) {
			throw new IllegalArgumentException("Generated ExpenseBillCreateRequest is null.");
		}

		if (!hasText(expenseBillRequest.getTenantId())) {
			throw new IllegalArgumentException("Generated ExpenseBillCreateRequest " + "has empty tenantId.");
		}

		if (!expenseBillRequest.getTenantId().equals(migrationRequest.getTenantId())) {
			throw new IllegalArgumentException("Generated request tenantId '" + expenseBillRequest.getTenantId()
					+ "' does not match migration tenantId '" + migrationRequest.getTenantId() + "'.");
		}

		if (expenseBillRequest.getExpenseBillRequest() == null) {
			throw new IllegalArgumentException("Generated ExpenseBillCreateRequest " + "has null expenseBillRequest.");
		}

		if (expenseBillRequest.getExpenseBillRequest().getEgBillregister() == null) {
			throw new IllegalArgumentException("Generated ExpenseBillRequest " + "has null egBillregister.");
		}

		if (record == null) {
			throw new IllegalArgumentException("Expense Bill record cannot be null " + "while validating generated request.");
		}
	}

	/*
	 * ================================================================ CREATE
	 * EXPENSE BILL ================================================================
	 */

	private ExpenseBillResponse createExpenseBill(ExpenseBillCreateRequest expenseBillRequest,ExpenseBillRecord record) {

		try {

			ExpenseBillResponse response = expenseBillApiClient.createExpenseBill(expenseBillRequest);
			if (response == null) {
				throw new IllegalArgumentException("Expense Bill API returned null response.");
			}
			return response;

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Expense Bill API call failed for SN " + record.getSerialNumber() + ": " + getExceptionMessage(e),e);
		}
	}

	/*
	 * ================================================================ VALIDATE API
	 * RESPONSE ================================================================
	 */

	private void validateApiResponse(ExpenseBillResponse response, ExpenseBillRecord record) {

		if (response == null) {
			throw new IllegalArgumentException("Expense Bill API returned null response for SN " + record.getSerialNumber() + ".");
		}

		/*
		 * Add validation according to your actual ExpenseBillResponse structure.
		 *
		 * Example:
		 *
		 * if (response.getResponseInfo() == null) { throw new IllegalArgumentException(
		 * "Expense Bill API responseInfo is null."); }
		 *
		 * if (response.getErrors() != null && !response.getErrors().isEmpty()) {
		 *
		 * throw new IllegalArgumentException( "Expense Bill API returned error: " +
		 * response.getErrors()); }
		 */
	}

	/*
	 * ================================================================ UPDATE JOB
	 * PROGRESS ================================================================
	 */

	private void updateJobProgress(MigrationJob job, int currentRecord, int totalRecords, int success, int failed,
			int skipped, String message) {

		if (job == null) {
			throw new IllegalArgumentException("Migration job cannot be null " + "while updating progress.");
		}

		if (totalRecords <= 0) {
			throw new IllegalArgumentException("Total records must be greater than zero.");
		}

		if (currentRecord < 0 || currentRecord > totalRecords) {
			throw new IllegalArgumentException("Invalid current record " + currentRecord + ". Total records=" + totalRecords + ".");
		}

		if (success < 0 || failed < 0 || skipped < 0) {
			throw new IllegalArgumentException("Success, failed and skipped counts " + "cannot be negative.");
		}

		if (success + failed + skipped != currentRecord) {
			throw new IllegalArgumentException("Migration progress count mismatch. " + "CurrentRecord=" + currentRecord
					+ ", Success=" + success + ", Failed=" + failed + ", Skipped=" + skipped);
		}

		if (!hasText(message)) {
			throw new IllegalArgumentException("Migration progress message cannot be empty.");
		}

		int progress = (int) (((double) currentRecord / totalRecords) * 100);

		if (progress < 0) {
			progress = 0;
		}

		if (progress > 100) {
			progress = 100;
		}

		job.setCurrentRecord(currentRecord);
		job.setTotalRecords(totalRecords);
		job.setProgressPercent(progress);
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setCurrentMessage(message);

		try {
			migrationJobRepository.saveAndFlush(job);
			progressPublisher.publish(job);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to update migration job progress: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ SAVE
	 * MIGRATION DETAIL
	 * ================================================================
	 */

	private void saveMigrationDetail(MigrationJob job, MigrationRequest request, RecordResult result, String recordKey) {

		if (job == null) {
			throw new IllegalArgumentException("Migration job cannot be null " + "while saving migration detail.");
		}

		if (request == null) {
			throw new IllegalArgumentException("Migration request cannot be null " + "while saving migration detail.");
		}

		if (result == null) {
			throw new IllegalArgumentException("Record result cannot be null " + "while saving migration detail.");
		}

		if (result.getStatus() == null) {
			throw new IllegalArgumentException("Record status cannot be null " + "while saving migration detail.");
		}

		if (!hasText(request.getTenantId())) {
			throw new IllegalArgumentException("Tenant ID cannot be empty " + "while saving migration detail.");
		}

		if (request.getMigrationType() == null) {
			throw new IllegalArgumentException("Migration type cannot be null " + "while saving migration detail.");
		}

		MigrationJobDetail detail = new MigrationJobDetail();
		detail.setJob(job);
		detail.setTenantId(request.getTenantId());
		detail.setModuleCode(request.getMigrationType().name());
		detail.setRecordNumber(result.getRecordNumber());
		detail.setStartRow(result.getStartRow());
		detail.setEndRow(result.getEndRow());
		detail.setStatus(result.getStatus().name());
		detail.setMessage(result.getMessage());
		detail.setExecutionTime(result.getExecutionTime());
		detail.setRecordKey(recordKey);
		detail.setCreatedTime(LocalDateTime.now());

		try {
			migrationJobDetailRepository.save(detail);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to save migration detail " + "for record "
					+ result.getRecordNumber() + ": " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ FINAL COUNTS
	 * VALIDATION ================================================================
	 */

	private void validateFinalCounts(int total, int success, int failed, int skipped) {
		if (total < 0) {
			throw new IllegalArgumentException("Total record count cannot be negative.");
		}

		if (success < 0 || failed < 0 || skipped < 0) {
			throw new IllegalArgumentException("Migration result counts cannot be negative.");
		}

		if (success + failed + skipped != total) {
			throw new IllegalArgumentException("Migration result count mismatch. " + "Total=" + total + ", Success="
					+ success + ", Failed=" + failed + ", Skipped=" + skipped + ".");
		}
	}

	/*
	 * ================================================================ FINAL JOB
	 * SAVE ================================================================
	 */

	private void saveFinalJob(MigrationJob job) {

		if (job == null) {
			throw new IllegalArgumentException("Migration job cannot be null " + "while saving final status.");
		}

		try {
			migrationJobRepository.save(job);
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to save final migration job status: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ FINAL
	 * MESSAGE ================================================================
	 */

	private String buildFinalMessage(int success, int failed, int skipped) {

		if (failed > 0) {
			return "Expense Bill migration completed with " + failed + " failed record(s).";

		}

		if (skipped > 0) {
			return "Expense Bill migration completed successfully. " + skipped + " record(s) skipped as duplicate.";

		}
		return "Expense Bill migration completed successfully.";
	}

	/*
	 * ================================================================ RECORD ERROR MESSAGE ================================================================
	 */

	private String buildRecordErrorMessage(ExpenseBillRecord record, Exception exception) {
		String message = getExceptionMessage(exception);

		if (record == null) {
			return "Expense Bill record processing failed: " + message;
		}

		return "Expense Bill processing failed for SN " + record.getSerialNumber() + " [Excel rows "
				+ record.getStartRow() + "-" + record.getEndRow() + "]: " + message;
	}

	/*
	 * ================================================================ UNEXPECTED
	 * ERROR MESSAGE
	 * ================================================================
	 */

	private String buildUnexpectedErrorMessage(ExpenseBillRecord record, Exception exception) {
		String message = getExceptionMessage(exception);
		if (record == null) {
			return "Unexpected Expense Bill processing error: " + message;
		}

		return "Unexpected Expense Bill processing error for SN " + record.getSerialNumber() + " [Excel rows "
				+ record.getStartRow() + "-" + record.getEndRow() + "]: " + message;
	}

	/*
	 * ================================================================ EXCEPTION
	 * MESSAGE ================================================================
	 */

	private String getExceptionMessage(Throwable exception) {

		if (exception == null) {
			return "Unknown error.";
		}

		if (hasText(exception.getMessage())) {
			return exception.getMessage();
		}

		Throwable cause = exception.getCause();

		if (cause != null && hasText(cause.getMessage())) {
			return cause.getMessage();
		}

		return exception.getClass().getSimpleName();
	}

	/*
	 * ================================================================ JOB ID
	 * ================================================================
	 */

	private String requestJobId(MigrationJob job) {

		if (job == null || job.getJobId() == null) {
			return "unknown";
		}

		return String.valueOf(job.getJobId());
	}

	/*
	 * ================================================================ TEXT
	 * VALIDATION ================================================================
	 */

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	/*
	 * ================================================================ OBJECT
	 * VALIDATION ================================================================
	 */

	private static <T> T requireObject(T object, String objectName) {

		if (object == null) {
			throw new IllegalArgumentException(objectName + " cannot be null.");
		}

		return object;
	}
}