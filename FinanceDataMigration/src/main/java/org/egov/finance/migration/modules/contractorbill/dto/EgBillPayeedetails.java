package org.egov.finance.migration.modules.contractorbill.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class EgBillPayeedetails {
	
    private Integer accountDetailTypeId;
    private Integer accountDetailKeyId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private Integer recoveryId;
    private String narration;
}
