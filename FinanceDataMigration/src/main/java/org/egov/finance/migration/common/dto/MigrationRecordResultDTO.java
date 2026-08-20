package org.egov.finance.migration.common.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MigrationRecordResultDTO {
    private Integer recordNumber;
    private Integer startRow;
    private Integer endRow;
    private String status;
    private String message;
    private Long executionTime;
    private LocalDateTime createdTime;
}
