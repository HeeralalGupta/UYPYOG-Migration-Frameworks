package org.egov.finance.migration.modules.purchaseorder.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PurchaseOrderItemRecord {

    /*
     * Excel tracking
     */
    private int rowNumber;

    /*
     * Purchase Order reference
     */
    private String ulbName;
    private String orderNo;

    /*
     * Item information
     */
    private String itemDescription;
    private String unit;

    /*
     * Pricing information
     */
    private BigDecimal rate;
    private BigDecimal gst;
    private BigDecimal unitValueWithGst;
    private BigDecimal quantity;
    private BigDecimal netAmount;
}