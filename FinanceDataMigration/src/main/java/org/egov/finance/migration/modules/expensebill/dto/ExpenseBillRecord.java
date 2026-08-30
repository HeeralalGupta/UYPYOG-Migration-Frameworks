package org.egov.finance.migration.modules.expensebill.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseBillRecord {

	@JsonIgnore
	private int startRow;
	@JsonIgnore
	private int endRow;
	private Integer serialNumber;

	// Bill Details
	private String ulbName;
	private String billDate;
	private String fund;
	private String department;
	private String scheme;
	private String fundSource;
	private String function;
	private String narration;
	private String partyBillNo;
	private String partyBillDate;
	private String billSubType;

	// SubLedger Details
	private String subLedgerType;
	private String subLedgerMaster;

	// Transaction Details
	private List<ExpenseDebitRecord> debitDetails = new ArrayList<>();
	private List<ExpenseDeductionRecord> deductionDetails = new ArrayList<>();
	private ExpenseNetPayableRecord netPayableDetail;

	
}