package org.egov.finance.migration.modules.contractorbill.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class EgBillDetails {
	
    private BigDecimal functionid;
    private BigDecimal glcodeid;
    private BigDecimal debitamount;
    private BigDecimal creditamount;
    private String narration;
}
