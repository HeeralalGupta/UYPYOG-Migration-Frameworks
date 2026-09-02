package org.egov.finance.migration.common.dto;

import java.util.List;

import lombok.Data;

@Data
public class SettingsResponse {

    private String applicationName;

    private String applicationStatus;

    private String databaseStatus;

    private String migrationEngineStatus;

    private Integer totalModules;

    private Integer activeJobs;

    private Integer tenantCount;

    private List<String> modules;

    private List<String> tenants;

    private Integer defaultPageSize;

    private Integer autoRefreshSeconds;

    private Integer maxUploadSizeMb;

    private String allowedFileExtensions;

    private String duplicatePolicy;

    private Integer concurrentMigrationLimit;
}