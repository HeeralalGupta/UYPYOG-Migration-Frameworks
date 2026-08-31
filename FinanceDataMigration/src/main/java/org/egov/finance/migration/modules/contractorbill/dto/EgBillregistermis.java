package org.egov.finance.migration.modules.contractorbill.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class EgBillregistermis {

    private BigDecimal segmentid;
    private BigDecimal subsegmentid;
    private String paybydate;
    private BigDecimal fieldid;
    private BigDecimal subfieldid;
    private Integer functionaryid;
    private String sanctionedby;
    private String sanctiondate;
    private String sanctiondetail;
    private String narration;
    private String lastupdatedtime;
    private String disbursementtype;
    private BigDecimal escalation;
    private BigDecimal advancepayments;
    private BigDecimal securedadvances;
    private BigDecimal deductamountwitheld;
    private BigDecimal month;
    private String departmentcode;
    private String departmentName;

    /*
     * These are actual JSON properties accepted by Finance.
     */
    private IdReference fundsource;
    private IdReference fund;
    private IdReference financialyear;

    /*
     * Finance expects:
     * "egBillSubType"
     */
    private IdReference egBillSubType;
    private String payto;
    private String mbRefNo;
    private IdReference function;

    /*
     * Finance exposes these two as transient JSON properties.
     */
    private Long schemeId;
    private Long subSchemeId;
    private String sourcePath;
    private String partyBillNumber;
    private String partyBillDate;
    private String inwardSerialNumber;
    private String budgetaryAppnumber;
    private Boolean budgetCheckReq;
}

