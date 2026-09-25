package org.egov.finance.migration.modules.supplierbill.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EgBillDetails {

	private BigDecimal functionid;
	private BigDecimal glcodeid;
	private BigDecimal debitamount;
	private BigDecimal creditamount;
	private String narration;
}
