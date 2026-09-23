package org.egov.finance.migration.service;

import org.egov.finance.migration.common.dto.MigrationProgress;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "MIGRATION_PROGRESS")
@Service
public class MigrationProgressPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public MigrationProgressPublisher(
            SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publishes the current migration progress to the browser.
     */
    public void publish(MigrationJob job) {

        MigrationProgress progress = MigrationProgress.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .totalRecords(job.getTotalRecords())
                .currentRecord(job.getCurrentRecord())
                .progressPercent(job.getProgressPercent())
                .successRecords(job.getSuccessRecords())
                .failedRecords(job.getFailedRecords())
                .skippedRecords(job.getSkippedRecords())
                .currentMessage(job.getCurrentMessage())
                .build();
        
        log.debug(
                "WS publish: jobId={}, destination=/topic/migration/{}, progress={}%, currentRecord={}, status={}",
                job.getJobId(),
                job.getJobId(),
                job.getProgressPercent(),
                job.getCurrentRecord(),
                job.getStatus()
        );

        messagingTemplate.convertAndSend(
                "/topic/migration/" + job.getJobId(),
                progress
        );
    }
}