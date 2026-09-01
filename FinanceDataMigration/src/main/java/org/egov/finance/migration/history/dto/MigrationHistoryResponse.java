package org.egov.finance.migration.history.dto;

import java.util.List;

import lombok.Data;

@Data
public class MigrationHistoryResponse {

    private Long totalJobs;

    private Long successfulJobs;

    private Long failedJobs;

    private Long totalRecords;

    private List<MigrationHistoryJob> jobs;

    private Integer page;

    private Integer pageSize;

    private Long totalPages;
}