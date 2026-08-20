package org.egov.finance.migration.common.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationResult {

    private boolean success;
    private String message;
    private Integer totalRecords;
    private Integer successRecords;
    private Integer failedRecords;
    private Integer skippedRecords;
    private Long totalExecutionTime;
    private List<RecordResult> recordResults;

}
