package org.egov.finance.migration.modules.supplierbill.service;

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
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillCreateRequest;
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillRecord;
import org.egov.finance.migration.modules.supplierbill.reader.SupplierBillExcelReader;
import org.egov.finance.migration.modules.supplierbill.response.SupplierBillResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;
import org.springframework.stereotype.Service;

@Service
public class SupplierBillMigrationProcessor extends AbstractMigrationProcessor {

	private final SupplierBillExcelReader excelReader;
	private final SupplierBillRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final SupplierBillApiClient supplierBillApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public SupplierBillMigrationProcessor(SupplierBillExcelReader excelReader,
			SupplierBillRequestBuilder requestBuilder, DuplicateDetectionService duplicateDetectionService,
			SupplierBillApiClient supplierBillApiClient, MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository, MigrationCancellationManager cancellationManager,
			MigrationProgressPublisher progressPublisher) {

		this.excelReader = requireObject(excelReader, "SupplierBillExcelReader");
		this.requestBuilder = requireObject(requestBuilder, "SupplierBillRequestBuilder");
		this.duplicateDetectionService = requireObject(duplicateDetectionService, "DuplicateDetectionService");
		this.supplierBillApiClient = requireObject(supplierBillApiClient, "SupplierBillApiClient");
		this.migrationJobRepository = requireObject(migrationJobRepository, "MigrationJobRepository");
		this.migrationJobDetailRepository = requireObject(migrationJobDetailRepository, "MigrationJobDetailRepository");
		this.cancellationManager = cancellationManager;
		this.progressPublisher = progressPublisher;
	}

	@Override
	public MigrationType getMigrationType() {
		return MigrationType.SUPPLIER_BILL;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();

		List<RecordResult> recordResults = new ArrayList<>();

		/*
		 * ============================================================ STEP 0 :
		 * VALIDATE MIGRATION REQUEST
		 * ============================================================
		 */

		validateMigrationRequest(request);

		/*
		 * ============================================================ STEP 1 : READ
		 * EXCEL ============================================================
		 */

		List<SupplierBillRecord> records = readExcel(request);

		if (records.isEmpty()) {
			throw new IllegalArgumentException("Supplier Bill migration failed: " + "No Supplier Bill records found in Excel file.");
		}

		/*
		 * ============================================================ STEP 2 : GET
		 * MIGRATION JOB ============================================================
		 */

		MigrationJob job = getMigrationJob(request);

		/*
		 * ============================================================ STEP 3 :
		 * INITIALIZE JOB ============================================================
		 */

		initializeJob(job, records.size());

		int success = 0;
		int failed = 0;
		int skipped = 0;

		/*
		 * ============================================================ STEP 4 : PROCESS
		 * EACH SUPPLIER BILL
		 * ============================================================
		 */

		for (int i = 0; i < records.size(); i++) {

			/*
			 * ======================================================== CHECK CANCELLATION
			 * ========================================================
			 */

			if (cancellationManager.isCancelled(request.getJobId())) {

				job.setStatus("CANCELLED");
				job.setCurrentMessage("Migration cancelled by user.");

				migrationJobRepository.saveAndFlush(job);
				progressPublisher.publish(job);

				break;
			}

			SupplierBillRecord record = records.get(i);

			long recordStart = System.currentTimeMillis();

			RecordResult result = new RecordResult();
			result.setRecordNumber(i + 1);

			if (record != null) {
				result.setStartRow(record.getStartRow());
				result.setEndRow(record.getEndRow());
			}

			List<String> recordKeys = getRecordKeys(record);

			try {

				/*
				 * ==================================================== 4.1 VALIDATE RECORD
				 * ====================================================
				 */

				validateRecord(record, i);

				/*
				 * ==================================================== 4.2 DUPLICATE CHECK
				 * ====================================================
				 */

				boolean alreadyMigrated = checkDuplicate(request, record);

				if (alreadyMigrated) {
					result.setStatus(RecordStatus.SKIPPED);
					result.setMessage("Supplier bill already migrated for " + "Excel rows " + record.getStartRow() + "-"
							+ record.getEndRow() + ".");

					skipped++;

				} else {

					SupplierBillCreateRequest supplierBillRequest = buildRequest(record, request);
					validateBuiltRequest(supplierBillRequest, record, request);
					SupplierBillResponse response = createSupplierBill(supplierBillRequest, record);
					validateApiResponse(response, record);

					result.setStatus(RecordStatus.SUCCESS);
					result.setMessage("Supplier bill created successfully.");

					success++;
				}

			} catch (IllegalArgumentException e) {

				/*
				 * Validation/business exception.
				 * Preserve the detailed message generated by SupplierBillRequestBuilder.
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


			recordResults.add(result);
			saveMigrationDetail(job, request, result, recordKeys);

			/*
			 * ======================================================== UPDATE JOB PROGRESS
			 * ========================================================
			 */

			updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
					"Processing Supplier Bill " + (i + 1) + " of " + records.size());
		}

		/*
		 * ============================================================ STEP 5 : HANDLE
		 * CANCELLATION ============================================================
		 */

		if ("CANCELLED".equals(job.getStatus())) {

			job.setCompletedTime(LocalDateTime.now());
			migrationJobRepository.save(job);
			cancellationManager.remove(request.getJobId());

			return MigrationResult.builder().success(false).message("Migration cancelled by user.")
					.totalRecords(records.size()).successRecords(success).failedRecords(failed).skippedRecords(skipped)
					.recordResults(recordResults).totalExecutionTime(System.currentTimeMillis() - startTime).build();
		}

		/*
		 * ============================================================ STEP 6 :
		 * VALIDATE FINAL COUNTS
		 * ============================================================
		 */

		validateFinalCounts(records.size(), success, failed, skipped);

		/*
		 * ============================================================ STEP 7 : FINAL
		 * JOB STATUS ============================================================
		 */

		String finalMessage = buildFinalMessage(success, failed, skipped);

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
		 * ============================================================ STEP 8 : RETURN
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
			throw new IllegalArgumentException("Tenant ID is required for Supplier Bill migration.");
		}

		if (request.getJobId() == null) {
			throw new IllegalArgumentException("Migration Job ID is required.");
		}

		if (request.getMigrationType() == null) {
			throw new IllegalArgumentException("Migration type is required.");
		}

		if (request.getMigrationType() != MigrationType.SUPPLIER_BILL) {
			throw new IllegalArgumentException("Invalid migration type. Expected " + MigrationType.SUPPLIER_BILL
					+ " but received " + request.getMigrationType() + ".");
		}

		if (request.getFile() == null) {
			throw new IllegalArgumentException("Supplier Bill Excel file is required.");
		}
	}

	/*
	 * ================================================================ READ EXCEL
	 * ================================================================
	 */

	private List<SupplierBillRecord> readExcel(MigrationRequest request) {

		try {
			List<SupplierBillRecord> records = excelReader.read(request.getFilePath());
			if (records == null) {
				throw new IllegalArgumentException("SupplierBillExcelReader returned null.");
			}

			return records;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to read Supplier Bill Excel file: " + getExceptionMessage(e), e);
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
			throw new IllegalArgumentException("Total Supplier Bill records must be greater than zero.");
		}

		job.setTotalRecords(totalRecords);
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");

		job.setCurrentMessage("Supplier Bill Excel read successfully. " + "Starting migration...");

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

	private void validateRecord(SupplierBillRecord record, int index) {

		if (record == null) {
			throw new IllegalArgumentException("Supplier Bill record at index " + index + " is null.");
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

	private boolean checkDuplicate(MigrationRequest request, SupplierBillRecord record) {

		List<String> recordKeys = getRecordKeys(record);

		try {

			return duplicateDetectionService.isAlreadyMigrated(request.getTenantId(), request.getMigrationType().name(),
					recordKeys);

		} catch (Exception e) {

			throw new IllegalArgumentException("Duplicate check failed for Supplier Bill SN " + record.getSerialNumber()
					+ " [rows " + record.getStartRow() + "-" + record.getEndRow() + "]: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ BUILD
	 * REQUEST ================================================================
	 */

	private SupplierBillCreateRequest buildRequest(SupplierBillRecord record, MigrationRequest request) {

		try {

			SupplierBillCreateRequest supplierBillRequest = requestBuilder.build(record, request);

			if (supplierBillRequest == null) {
				throw new IllegalArgumentException("SupplierBillRequestBuilder returned null.");
			}

			return supplierBillRequest;

		} catch (IllegalArgumentException e) {

			/*
			 * Preserve detailed validation messages generated by
			 * SupplierBillRequestBuilder.
			 */

			throw e;

		} catch (Exception e) {

			throw new IllegalArgumentException("Unexpected error while building " + "Supplier Bill request: " + getExceptionMessage(e), e);
		}
	}

	/*
	 * ================================================================ VALIDATE
	 * GENERATED REQUEST
	 * ================================================================
	 */

	private void validateBuiltRequest(SupplierBillCreateRequest supplierBillRequest, SupplierBillRecord record,
			MigrationRequest migrationRequest) {

		if (supplierBillRequest == null) {
			throw new IllegalArgumentException("Generated SupplierBillCreateRequest is null.");
		}

		if (!hasText(supplierBillRequest.getTenantId())) {
			throw new IllegalArgumentException("Generated SupplierBillCreateRequest " + "has empty tenantId.");
		}

		if (!supplierBillRequest.getTenantId().equals(migrationRequest.getTenantId())) {

			throw new IllegalArgumentException("Generated request tenantId '" + supplierBillRequest.getTenantId()
					+ "' does not match migration tenantId '" + migrationRequest.getTenantId() + "'.");
		}

		if (supplierBillRequest.getSupplierBillRequest() == null) {
			throw new IllegalArgumentException(
					"Generated SupplierBillCreateRequest " + "has null supplierBillRequest.");
		}

		if (supplierBillRequest.getSupplierBillRequest().getEgBillregister() == null) {

			throw new IllegalArgumentException("Generated SupplierBillRequest " + "has null egBillregister.");
		}

		if (record == null) {
			throw new IllegalArgumentException(
					"Supplier Bill record cannot be null " + "while validating generated request.");
		}
	}

	/*
	 * ================================================================ CREATE
	 * SUPPLIER BILL
	 * ================================================================
	 */

	private SupplierBillResponse createSupplierBill(SupplierBillCreateRequest supplierBillRequest,
			SupplierBillRecord record) {

		try {

			SupplierBillResponse response = supplierBillApiClient.createSupplierBill(supplierBillRequest);

			if (response == null) {
				throw new IllegalArgumentException("Supplier Bill API returned null response.");
			}

			return response;

		} catch (IllegalArgumentException e) {

			throw e;

		} catch (Exception e) {

			throw new IllegalArgumentException(
					"Supplier Bill API call failed for SN " + record.getSerialNumber() + ": " + getExceptionMessage(e),
					e);
		}
	}

	/*
	 * ================================================================ VALIDATE API
	 * RESPONSE ================================================================
	 */

	private void validateApiResponse(SupplierBillResponse response, SupplierBillRecord record) {

		if (response == null) {
			throw new IllegalArgumentException("Supplier Bill API returned null response " + "for SN " + record.getSerialNumber() + ".");
		}

		/*
		 * Add validation according to your actual SupplierBillResponse structure.
		 *
		 * Example:
		 *
		 * if (response.getResponseInfo() == null) { throw new IllegalArgumentException(
		 * "Supplier Bill API responseInfo is null."); }
		 *
		 * if (response.getErrors() != null && !response.getErrors().isEmpty()) {
		 *
		 * throw new IllegalArgumentException( "Supplier Bill API returned error: " +
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

	private void saveMigrationDetail(MigrationJob job, MigrationRequest request, RecordResult result,List<String> recordKey) {

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
			throw new IllegalArgumentException("Unable to save final migration job status: " + getExceptionMessage(e),
					e);
		}
	}

	/*
	 * ================================================================ FINAL
	 * MESSAGE ================================================================
	 */

	private String buildFinalMessage(int success, int failed, int skipped) {

		if (failed > 0) {
			return "Supplier Bill migration completed with " + failed + " failed record(s).";
		}

		if (skipped > 0) {
			return "Supplier Bill migration completed successfully. " + skipped + " record(s) skipped as duplicate.";
		}

		return "Supplier Bill migration completed successfully.";
	}

	/*
	 * ================================================================ RECORD ERROR
	 * MESSAGE ================================================================
	 */

	private String buildRecordErrorMessage(SupplierBillRecord record, Exception exception) {
		String message = getExceptionMessage(exception);

		if (record == null) {
			return "Supplier Bill record processing failed: " + message;
		}

		return "Supplier Bill processing failed for SN " + record.getSerialNumber() + " [Excel rows "
				+ record.getStartRow() + "-" + record.getEndRow() + "]: " + message;
	}

	/*
	 * ================================================================ UNEXPECTED
	 * ERROR MESSAGE
	 * ================================================================
	 */

	private String buildUnexpectedErrorMessage(SupplierBillRecord record, Exception exception) {

		String message = getExceptionMessage(exception);

		if (record == null) {
			return "Unexpected Supplier Bill processing error: " + message;
		}

		return "Unexpected Supplier Bill processing error for SN " + record.getSerialNumber() + " [Excel rows "
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

	private static <T> T requireObject( T object, String objectName) {

		if (object == null) {
			throw new IllegalArgumentException(objectName + " cannot be null.");
		}

		return object;
	}
}