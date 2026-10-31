package org.egov.finance.migration.modules.deductionpayment.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class DeductionPaymentRecord {
    
	private Integer serialNumber;
    private String ulbName;
    private Date voucherDate;
    private String voucherNumber;
    private String voucherType;
    private String recovery;
    private String fund;
    private BigDecimal deductionAmount;
    private Date paymentDate;
    private String bank;
    private String branch;
    private String bankAccountNumber;
    private String modeOfPayment;
    private String department;
    private String function;
    private int startRow;
    private int endRow;
}
