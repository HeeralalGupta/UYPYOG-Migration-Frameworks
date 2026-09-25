package org.egov.finance.migration.modules.supplierbill.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EgBillPayeedetails {
	
    private Integer accountDetailTypeId;
    private Integer accountDetailKeyId;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private Integer recoveryId;
    private String narration;
}
