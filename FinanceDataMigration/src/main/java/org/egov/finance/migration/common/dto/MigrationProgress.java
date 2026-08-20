package org.egov.finance.migration.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationProgress {

    private String jobId;

    private String status;

    private Integer totalRecords;

    private Integer currentRecord;

    private Integer progressPercent;

    private Integer successRecords;

    private Integer failedRecords;

    private Integer skippedRecords;

    private String currentMessage;
}
