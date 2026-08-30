package org.egov.finance.migration.modules.purchaseorder.service;

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
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRecord;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRequest;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderResponse;
import org.egov.finance.migration.modules.purchaseorder.reader.PurchaseOrderExcelReader;
import org.egov.finance.migration.processor.AbstractMigrationProcessor;
import org.egov.finance.migration.service.DuplicateDetectionService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderMigrationProcessor
        extends AbstractMigrationProcessor {

    private final PurchaseOrderExcelReader excelReader;

    private final PurchaseOrderRequestBuilder requestBuilder;

    private final DuplicateDetectionService duplicateDetectionService;

    private final PurchaseOrderApiClient purchaseOrderApiClient;

    private final MigrationJobRepository migrationJobRepository;

    private final MigrationJobDetailRepository migrationJobDetailRepository;

    private final RequestInfoBuilder requestInfoBuilder;

    public PurchaseOrderMigrationProcessor(
            PurchaseOrderExcelReader excelReader,
            PurchaseOrderRequestBuilder requestBuilder,
            DuplicateDetectionService duplicateDetectionService,
            PurchaseOrderApiClient purchaseOrderApiClient,
            MigrationJobRepository migrationJobRepository,
            MigrationJobDetailRepository migrationJobDetailRepository,
            RequestInfoBuilder requestInfoBuilder) {

        this.excelReader = excelReader;
        this.requestBuilder = requestBuilder;
        this.duplicateDetectionService = duplicateDetectionService;
        this.purchaseOrderApiClient = purchaseOrderApiClient;
        this.migrationJobRepository = migrationJobRepository;
        this.migrationJobDetailRepository = migrationJobDetailRepository;
        this.requestInfoBuilder = requestInfoBuilder;
    }

    @Override
    public MigrationType getMigrationType() {
        return MigrationType.PURCHASE_ORDER;
    }

    @Override
    protected MigrationResult doProcess(MigrationRequest request) {

        long startTime = System.currentTimeMillis();

        List<RecordResult> recordResults =
                new ArrayList<>();

        /*
         * ============================================================
         * STEP 1 : READ EXCEL
         * ============================================================
         */

        List<PurchaseOrderRecord> records =
                excelReader.read(request.getFile());

        /*
         * ============================================================
         * STEP 2 : GET EXISTING MIGRATION JOB
         * ============================================================
         */

        MigrationJob job =
                migrationJobRepository
                        .findByJobId(request.getJobId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
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
                "Excel read successfully. "
                        + "Starting Purchase Order migration...");

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
         * STEP 4 : PROCESS EACH PURCHASE ORDER INDEPENDENTLY
         *
         * One PurchaseOrderRecord = one Purchase Order API request.
         *
         * Purchase Order items are already attached to the
         * PurchaseOrderRecord by PurchaseOrderExcelReader.
         *
         * If one Purchase Order fails, remaining Purchase Orders
         * will still be processed.
         * ============================================================
         */

        for (int i = 0; i < records.size(); i++) {

            PurchaseOrderRecord record =
                    records.get(i);

            long recordStart =
                    System.currentTimeMillis();

            RecordResult result =
                    new RecordResult();

            /*
             * PurchaseOrderRecord contains one Excel
             * master row number.
             */

            result.setRecordNumber(i + 1);

            result.setStartRow(
                    record.getRowNumber());

            result.setEndRow(
                    record.getRowNumber());

            /*
             * ========================================================
             * DUPLICATE CHECK
             * ========================================================
             */

            boolean alreadyMigrated =
                    duplicateDetectionService
                            .isAlreadyMigrated(
                                    request.getTenantId(),
                                    request.getMigrationType().name(),
                                    record.getRowNumber(),
                                    record.getRowNumber());

            if (alreadyMigrated) {

                result.setStatus(
                        RecordStatus.SKIPPED);

                result.setMessage(
                        "Purchase Order already migrated.");

                result.setExecutionTime(0L);

                skipped++;

                recordResults.add(result);

                /*
                 * Save skipped record.
                 */

                saveMigrationDetail(
                        job,
                        request,
                        result,
                        RecordStatus.SKIPPED.name());

                /*
                 * Update progress.
                 */

                updateJobProgress(
                        job,
                        i + 1,
                        records.size(),
                        success,
                        failed,
                        skipped,
                        "Purchase Order "
                                + (i + 1)
                                + " of "
                                + records.size()
                                + " skipped - already migrated.");

                /*
                 * Do not process duplicate.
                 */

                continue;
            }

            /*
             * ========================================================
             * PROCESS CURRENT PURCHASE ORDER
             * ========================================================
             */

            try {

                /*
                 * Build PurchaseOrderRequest.
                 */

                PurchaseOrderRequest purchaseOrderRequest =
                        requestBuilder.build(record);

                if (purchaseOrderRequest == null) {

                    throw new RuntimeException(
                            "Unable to build PurchaseOrderRequest.");
                }

                /*
                 * Create API request.
                 */

                ApiRequest<PurchaseOrderRequest> apiRequest =
                        new ApiRequest<>();

                apiRequest.setTenantId(
                        request.getTenantId());

                apiRequest.setRequest(
                        purchaseOrderRequest);

                apiRequest.setRequestInfo(
                        requestInfoBuilder.build(
                                request.getTenantId()));

                /*
                 * Call Purchase Order Create API.
                 */

                ApiResponse<PurchaseOrderResponse> response =
                        purchaseOrderApiClient
                                .createPurchaseOrder(apiRequest);

                /*
                 * Validate API response.
                 */

                if (response == null) {

                    throw new RuntimeException(
                            "Purchase Order API returned "
                                    + "empty response.");
                }

                if (!response.isSuccess()) {

                    throw new RuntimeException(
                            response.getMessage() != null
                                    ? response.getMessage()
                                    : "Purchase Order API failed.");
                }

                /*
                 * Success.
                 */

                result.setStatus(
                        RecordStatus.SUCCESS);

                result.setMessage(
                        "Purchase Order created successfully.");

                success++;

            } catch (Exception e) {

                /*
                 * Failure of current record should not
                 * stop remaining records.
                 */

                result.setStatus(
                        RecordStatus.FAILED);

                String errorMessage =
                        getRootCauseMessage(e);

                result.setMessage(
                        errorMessage);

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
                    "Processing Purchase Order "
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
                    "Purchase Order migration completed with "
                            + failed
                            + " failed record(s).";

        } else if (skipped > 0) {

            finalMessage =
                    "Purchase Order migration completed successfully. "
                            + skipped
                            + " record(s) skipped as duplicate.";

        } else {

            finalMessage =
                    "Purchase Order migration completed successfully.";
        }

        job.setCurrentMessage(
                finalMessage);

        /*
         * ============================================================
         * FINAL STATUS
         * ============================================================
         */

        if (failed > 0) {

            job.setStatus(
                    "COMPLETED_WITH_ERRORS");

        } else {

            job.setStatus(
                    "COMPLETED");
        }

        job.setCompletedTime(
                LocalDateTime.now());

        migrationJobRepository.save(job);

        /*
         * ============================================================
         * TOTAL EXECUTION TIME
         * ============================================================
         */

        long totalExecutionTime =
                System.currentTimeMillis()
                        - startTime;

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
                .totalExecutionTime(
                        totalExecutionTime)
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

        job.setCurrentRecord(
                currentRecord);

        job.setTotalRecords(
                totalRecords);

        int progress = 0;

        if (totalRecords > 0) {

            progress = (int) (
                    ((double) currentRecord
                            / totalRecords) * 100
            );
        }

        job.setProgressPercent(
                progress);

        job.setSuccessRecords(
                success);

        job.setFailedRecords(
                failed);

        job.setSkippedRecords(
                skipped);

        job.setCurrentMessage(
                message);

        migrationJobRepository
                .saveAndFlush(job);
    }

    /**
     * Save migration detail for one
     * Purchase Order record.
     */
    private void saveMigrationDetail(
            MigrationJob job,
            MigrationRequest request,
            RecordResult result,
            String status) {

        MigrationJobDetail detail =
                new MigrationJobDetail();

        detail.setJob(job);

        detail.setTenantId(
                request.getTenantId());

        detail.setModuleCode(
                request.getMigrationType().name());

        detail.setRecordNumber(
                result.getRecordNumber());

        detail.setStartRow(
                result.getStartRow());

        detail.setEndRow(
                result.getEndRow());

        detail.setStatus(
                status);

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

        migrationJobDetailRepository
                .save(detail);
    }

    /**
     * Get the actual root cause message.
     */
    private String getRootCauseMessage(
            Throwable exception) {

        Throwable root =
                exception;

        while (root.getCause() != null) {

            root = root.getCause();
        }

        if (root.getMessage() == null) {

            return root.getClass()
                    .getSimpleName();
        }

        return root.getMessage();
    }
}