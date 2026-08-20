package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.common.enums.JobStatus;
import org.egov.finance.migration.common.enums.MigrationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationSummary {

    private String jobId;

    private MigrationType migrationType;

    private String tenantId;

    private Integer total;

    private Integer success;

    private Integer failed;

    private Integer skipped;

    private JobStatus status;

}
