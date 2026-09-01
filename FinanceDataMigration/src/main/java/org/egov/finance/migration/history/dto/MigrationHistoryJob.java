package org.egov.finance.migration.history.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MigrationHistoryJob {

    private String jobId;

    private String module;

    private String tenant;

    private String status;

    private Integer totalRecords;

    private Integer successRecords;

    private Integer failedRecords;

    private Integer skippedRecords;

    private Integer progressPercent;

    private String currentMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}