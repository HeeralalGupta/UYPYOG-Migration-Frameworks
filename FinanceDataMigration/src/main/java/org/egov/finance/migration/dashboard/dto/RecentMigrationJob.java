package org.egov.finance.migration.dashboard.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RecentMigrationJob {

    private String jobId;

    private String module;

    private String tenant;

    private String status;

    private Integer totalRecords;

    private Integer successRecords;

    private Integer failedRecords;

    private Integer skippedRecords;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private Integer progressPercent;

    private String currentMessage;
}