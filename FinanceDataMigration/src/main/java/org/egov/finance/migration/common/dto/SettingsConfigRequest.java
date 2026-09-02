package org.egov.finance.migration.common.dto;

import lombok.Data;

@Data
public class SettingsConfigRequest {

    private Integer defaultPageSize;

    private Integer autoRefreshSeconds;

    private Integer maxUploadSizeMb;

    private String allowedFileExtensions;

    private String duplicatePolicy;

    private Integer concurrentMigrationLimit;
}