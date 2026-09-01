package org.egov.finance.migration.common.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ReportsResponse {

    private Integer totalJobs;

    private Integer successfulJobs;

    private Integer failedJobs;

    private Integer runningJobs;

    private Integer skippedRecords;

    private Integer totalRecords;

    private Double successRate;

    private Double averageDurationSeconds;

    private List<String> trendLabels;

    private List<Integer> trendSuccessful;

    private List<Integer> trendFailed;

    private List<Integer> trendRunning;

    private Map<String, Integer> moduleJobs;

    private Map<String, Integer> tenantJobs;
}