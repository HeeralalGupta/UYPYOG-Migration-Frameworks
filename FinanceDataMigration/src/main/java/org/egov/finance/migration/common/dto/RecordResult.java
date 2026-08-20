package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.common.enums.RecordStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordResult {

    private Integer recordNumber;
    private Integer startRow;
    private Integer endRow;
    private RecordStatus status;
    private String message;
    private Long executionTime;
    private String recordKey;

}
