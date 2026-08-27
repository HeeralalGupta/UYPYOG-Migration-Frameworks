package org.egov.finance.migration.modules.work.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class WorkRecord {

    /*
     * Excel tracking
     */
    private int rowNumber;

    /*
     * Work information
     */
    private String ulbName;
    private String nameOfWork;
    private String workType;
    private String fund;
    private BigDecimal estimateValue;
    private Date startDate;
    private Date endDate;
}