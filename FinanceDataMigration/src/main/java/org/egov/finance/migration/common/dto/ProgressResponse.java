package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.common.enums.JobStatus;
import org.egov.finance.migration.common.enums.MigrationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponse {

    private Integer progress;
    private MigrationStatus migrationStatus;
    private JobStatus jobStatus;
    private Integer totalRecords;
    private Integer successRecords;
    private Integer failedRecords;
    private Integer skippedRecords;
    private String message;

}
