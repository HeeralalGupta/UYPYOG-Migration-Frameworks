package org.egov.finance.migration.modules.workorder.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WorkOrderItemRecord {

    /*
     * Excel tracking
     */
    private int rowNumber;

    /*
     * Work Order reference
     */
    private String tenderNumber;
    private String workOrderNo;

    /*
     * Item information
     */
    private String itemName;
    private String glCode;
    private String unit;

    /*
     * Pricing information
     */
    private BigDecimal unitRate;
    private BigDecimal gst;
    private BigDecimal unitValueWithGst;
    private BigDecimal quantity;
    private BigDecimal amount;
}