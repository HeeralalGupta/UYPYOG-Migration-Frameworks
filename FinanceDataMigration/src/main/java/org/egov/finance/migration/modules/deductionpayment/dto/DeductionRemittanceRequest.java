package org.egov.finance.migration.modules.deductionpayment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class DeductionRemittanceRequest {

	private LocalDate referenceVoucherDate;
	private String referenceVoucherNumber;
	private String referenceVoucherType;
	private String recovery;
	private String fund;
	private BigDecimal deductionAmount;
	private LocalDate paymentDate;
	private String bank;
	private String branch;
	private String bankAccountNumber;
	private String modeOfPayment;
	private String department;
	private String function;
	private String scheme;
	private String subscheme;

}
