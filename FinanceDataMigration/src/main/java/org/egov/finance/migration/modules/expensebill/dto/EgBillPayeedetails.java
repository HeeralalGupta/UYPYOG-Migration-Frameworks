package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EgBillPayeedetails {

	private EgBillDetailsIdDTO egBilldetailsId;

	private BigDecimal debitAmount;

	private BigDecimal creditAmount;

	private Boolean isDebit;

	private Long accountDetailTypeId;

	private Long accountDetailKeyId;

}