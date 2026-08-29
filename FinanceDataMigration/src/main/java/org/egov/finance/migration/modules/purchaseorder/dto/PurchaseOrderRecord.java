package org.egov.finance.migration.modules.purchaseorder.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class PurchaseOrderRecord {

    /*
     * Excel tracking
     */
    private int rowNumber;

    /*
     * Purchase Order information
     */
    private String ulbName;
    private String orderNo;
    private Date orderDate;
    private String orderName;
    private String description;

    /*
     * Supplier information
     */
    private String supplierName;

    /*
     * Accounting classification
     */
    private String fund;
    private String department;
    private String scheme;
    private String subScheme;

    /*
     * Approval information
     */
    private String sanctionNo;
    private Date sanctionDate;

    /*
     * Financial information
     */
    private BigDecimal advancePayable;
    private BigDecimal totalOrderValue;

    /*
     * Purchase Order Items
     */
    private List<PurchaseOrderItemRecord> items;
}