package org.egov.finance.migration.modules.banktobank.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;

@Data
public class BankToBankTransferRecord {

	/*
	 * Excel tracking
	 */
	private int rowNumber;
	private Integer serialNumber;

	/*
	 * Transfer information
	 */
	private String ulbName;
	private Date voucherDate;

	/*
	 * Source bank details
	 */
	private String fromFund;
	private String fromSubScheme;
	private String fromDepartment;
	private String function;
	private String fromBank;
	private String fromAccountNumber;

	/*
	 * Destination bank details
	 */
	private String toFund;
	private String toDepartment;
	private String toBank;
	private String toAccountNumber;

	/*
	 * Inter-fund transfer details
	 */
	private String sourceInterFundCode;
	private String destinationInterFundCode;

	/*
	 * Transaction details
	 */
	private String modeOfTransaction;
	private String referenceNumber;
	private Date referenceDate;
	private BigDecimal amount;
	private String narration;
}