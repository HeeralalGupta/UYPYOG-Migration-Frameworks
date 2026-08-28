package org.egov.finance.migration.modules.workorder.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class WorkOrderRecord {

    /*
     * Excel tracking
     */
    private int rowNumber;

    /*
     * Work Order information
     */
    private String ulbName;
    private String tenderNumber;
    private String workOrderNo;
    private Date workOrderDate;
    private String workOrderName;
    private String workOrderType;
    private String description;
    private String active;

    /*
     * Contractor and Work information
     */
    private String contractorName;
    private String workName;
    private String workCode;

    /*
     * Financial information
     */
    private BigDecimal totalOrderAmt;
    private BigDecimal advancePayable;

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
    private String workOrderIssuingAuthority;
    private Date sanctionDate;

    /*
     * Security / Deposit information
     */
    private BigDecimal emdAmount;
    private BigDecimal bgAmount;
    private BigDecimal apbgAmount;

    /*
     * Work Order Items
     */
    private List<WorkOrderItemRecord> items;
}