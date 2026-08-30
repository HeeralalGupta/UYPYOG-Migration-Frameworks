package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseNetPayableRecord {

	private String glCode;

	private BigDecimal creditAmount;

}