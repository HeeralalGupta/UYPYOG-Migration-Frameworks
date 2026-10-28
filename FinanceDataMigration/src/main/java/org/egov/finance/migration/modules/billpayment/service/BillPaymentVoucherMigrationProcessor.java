
package org.egov.finance.migration.modules.billpayment.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.MigrationResult;
import org.egov.finance.migration.common.dto.RecordResult;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.entity.MigrationJobDetail;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.common.enums.RecordStatus;
import org.egov.finance.migration.common.repository.MigrationJobDetailRepository;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.modules.billpayment.dto.BillPaymentVoucherRecord;
import org.egov.finance.migration.modules.billpayment.dto.PaymentBillRecord;
import org.egov.finance.migration.modules.billpayment.dto.PaymentCreateRequest;
import org.egov.finance.migration.modules.billpayment.reader.BillPaymentVoucherExcelReader;
import org.egov.finance.migration.modules.billpayment.response.BillPaymentVoucherResponse;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;
import org.springframework.stereotype.Service;

@Service
public class BillPaymentVoucherMigrationProcessor extends AbstractMigrationProcessor {

	private final BillPaymentVoucherExcelReader excelReader;
	private final BillPaymentVoucherRequestBuilder requestBuilder;
	private final DuplicateDetectionService duplicateDetectionService;
	private final BillPaymentVoucherApiClient billPaymentVoucherApiClient;
	private final MigrationJobRepository migrationJobRepository;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public BillPaymentVoucherMigrationProcessor(BillPaymentVoucherExcelReader excelReader,
			BillPaymentVoucherRequestBuilder requestBuilder, DuplicateDetectionService duplicateDetectionService,
			BillPaymentVoucherApiClient billPaymentVoucherApiClient, MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository, MigrationCancellationManager cancellationManager,
			MigrationProgressPublisher progressPublisher) {

		this.excelReader = excelReader;
		this.requestBuilder = requestBuilder;
		this.duplicateDetectionService = duplicateDetectionService;
		this.billPaymentVoucherApiClient = billPaymentVoucherApiClient;
		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
		this.cancellationManager = cancellationManager;
		this.progressPublisher = progressPublisher;
	}

	@Override
	public MigrationType getMigrationType() {
		return MigrationType.BILL_PAYMENT_VOUCHER;
	}

	@Override
	protected MigrationResult doProcess(MigrationRequest request) {

		long startTime = System.currentTimeMillis();
		List<RecordResult> recordResults = new ArrayList<>();

		// Step 1: Read Excel records.
		List<BillPaymentVoucherRecord> records = excelReader.read(request.getFilePath());

		if (records == null) {
			throw new IllegalArgumentException("Unable to read Bill Payment Voucher Excel file.");
		}

		// Step 2: Get migration job.
		MigrationJob job = migrationJobRepository.findByJobId(request.getJobId())
				.orElseThrow(() -> new IllegalArgumentException("Migration job not found: " + request.getJobId()));

		// Step 3: Initialize job progress.
		job.setTotalRecords(records.size());
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setStatus("RUNNING");
		job.setCurrentMessage("Excel read successfully. Starting Bill Payment Voucher migration...");

		migrationJobRepository.save(job);

		int success = 0;
		int failed = 0;
		int skipped = 0;

		/*
		 * One BillPaymentVoucherRecord represents one payment voucher. A voucher may
		 * contain multiple bill numbers and payment amounts.
		 */
		for (int i = 0; i < records.size(); i++) {

			// Step 4: Check whether the job has been cancelled.
			if (cancellationManager.isCancelled(request.getJobId())) {

				job.setStatus("CANCELLED");
				job.setCurrentMessage("Migration cancelled by user.");

				migrationJobRepository.saveAndFlush(job);
				progressPublisher.publish(job);

				break;
			}

			BillPaymentVoucherRecord record = records.get(i);
			long recordStart = System.currentTimeMillis();

			RecordResult result = new RecordResult();
			result.setRecordNumber(i + 1);
			result.setStartRow(record.getStartRow());
			result.setEndRow(record.getEndRow());

			List<String> recordKeys = getRecordKeys(record);

			// Step 5: Check for duplicates.
			boolean alreadyMigrated = duplicateDetectionService.isAlreadyMigrated(request.getTenantId(),
					request.getMigrationType().name(), recordKeys);

			if (alreadyMigrated) {

				result.setStatus(RecordStatus.SKIPPED);
				result.setMessage("Bill Payment Voucher already migrated.");
				result.setExecutionTime(0L);

				skipped++;
				recordResults.add(result);

				saveMigrationDetail(job, request, result, RecordStatus.SKIPPED.name(), recordKeys);

				updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
						"Bill Payment Voucher " + (i + 1) + " of " + records.size() + " skipped - already migrated.");

				continue;
			}

			// Step 6: Build the request and call the API.
			try {

				PaymentCreateRequest paymentRequest = requestBuilder.build(record, request);

				if (paymentRequest == null) {
					throw new IllegalStateException("Unable to build Bill Payment Voucher request.");
				}

				if (paymentRequest.getBills() == null || paymentRequest.getBills().isEmpty()) {
					throw new IllegalStateException("Payment voucher must contain at least one bill.");
				}

				/*
				 * IMPORTANT: Pass paymentRequest, not MigrationRequest.
				 *
				 * The API client must wrap this DTO in the API envelope containing RequestInfo,
				 * tenantId and request.
				 */
				ApiRequest<PaymentCreateRequest> apiRequest = requestBuilder.buildApiRequest(paymentRequest, request);

				BillPaymentVoucherResponse response = billPaymentVoucherApiClient.createPayment(apiRequest,
						request.getTenantId());

				if (response == null) {
					throw new IllegalStateException("Bill Payment Voucher API returned an empty response.");
				}

				result.setStatus(RecordStatus.SUCCESS);
				result.setMessage("Bill Payment Voucher created successfully.");

				success++;

			} catch (Exception e) {

				result.setStatus(RecordStatus.FAILED);
				result.setMessage(getRootCauseMessage(e));

				failed++;

			} finally {
				result.setExecutionTime(System.currentTimeMillis() - recordStart);
			}

			// Step 7: Save the record result.
			recordResults.add(result);

			saveMigrationDetail(job, request, result, result.getStatus().name(), recordKeys);

			// Step 8: Publish progress.
			updateJobProgress(job, i + 1, records.size(), success, failed, skipped,
					"Processing Bill Payment Voucher " + (i + 1) + " of " + records.size());
		}

		// Step 9: Handle cancellation.
		if ("CANCELLED".equals(job.getStatus())) {

			job.setCompletedTime(LocalDateTime.now());
			migrationJobRepository.save(job);

			cancellationManager.remove(request.getJobId());

			return MigrationResult.builder().success(false).message("Migration cancelled by user.")
					.totalRecords(records.size()).successRecords(success).failedRecords(failed).skippedRecords(skipped)
					.recordResults(recordResults).totalExecutionTime(System.currentTimeMillis() - startTime).build();
		}

		// Step 10: Update final counts.
		job.setTotalRecords(records.size());
		job.setSuccessRecords(success);
		job.setFailedRecords(failed);
		job.setSkippedRecords(skipped);
		job.setProgressPercent(100);
		job.setCurrentRecord(records.size());

		String finalMessage;

		if (failed > 0) {
			finalMessage = "Migration completed with " + failed + " failed record(s).";
		} else if (skipped > 0) {
			finalMessage = "Migration completed successfully. " + skipped + " record(s) skipped as duplicate.";
		} else {
			finalMessage = "Migration completed successfully.";
		}

		job.setCurrentMessage(finalMessage);

		if (failed > 0) {
			job.setStatus("COMPLETED_WITH_ERRORS");
		} else {
			job.setStatus("COMPLETED");
		}

		job.setCompletedTime(LocalDateTime.now());

		migrationJobRepository.save(job);
		progressPublisher.publish(job);

		// Step 11: Return final migration result.
		long totalExecutionTime = System.currentTimeMillis() - startTime;

		return MigrationResult.builder().success(failed == 0).message(finalMessage).totalRecords(records.size())
				.successRecords(success).failedRecords(failed).skippedRecords(skipped).recordResults(recordResults)
				.totalExecutionTime(totalExecutionTime).build();
	}

	/**
	 * Update job counts and publish real-time progress.
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
	 * Persist an individual voucher's migration result.
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
	 * Extract the most specific available error message.
	 */
	private String getRootCauseMessage(Throwable exception) {

		Throwable root = exception;

		while (root.getCause() != null) {
			root = root.getCause();
		}

		if (root.getMessage() == null || root.getMessage().trim().isEmpty()) {
			return root.getClass().getSimpleName();
		}

		return root.getMessage();
	}

	/**
	 * Generate duplicate detection keys using only Bill Number. Each Excel row
	 * represents one payment voucher with one bill.
	 */
	private List<String> getRecordKeys(BillPaymentVoucherRecord record) {
	    List<String> keys = new ArrayList<>();

	    if (record.getBills() != null) {
	        record.getBills().stream()
	                .map(PaymentBillRecord::getBillNumber)
	                .filter(number -> number != null && !number.trim().isEmpty())
	                .map(String::trim)
	                .distinct()
	                .forEach(number -> keys.add("BILL:" + number));
	    }

	    return keys;
	}
}
