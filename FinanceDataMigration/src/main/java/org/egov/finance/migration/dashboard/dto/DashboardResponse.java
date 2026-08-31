package org.egov.finance.migration.dashboard.dto;

import java.util.List;

import lombok.Data;

@Data
public class DashboardResponse {

    private Integer migrationModules;
    private Long totalJobs;
    private Long todayJobs;
    private Double successRate;
    private Long successfulJobs;
    private Long failedJobs;
    private Long runningJobs;
    private List<RecentMigrationJob> recentJobs;
}