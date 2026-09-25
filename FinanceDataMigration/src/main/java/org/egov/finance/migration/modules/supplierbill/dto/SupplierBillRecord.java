package org.egov.finance.migration.modules.supplierbill.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierBillRecord {

	/*
	 * Excel tracking
	 */
	private int startRow;
	private int endRow;
	private Integer serialNumber;
	/*
	 * Main Bill Information
	 */
	private String ulbName;
	private String billDate;
	private String supplier;
	private String purchaseOrder;
	private String fund;
	private String department;
	private String scheme;
	private String subScheme;
	private String fundSource;
	private String function;
	private String narration;
	private String partyBillNo;
	private String partyBillDate;
	private BigDecimal partyBillAmount;
	private String billType;

	/*
	 * Bill Details
	 */
	private List<EgBillDetails> debitDetails = new ArrayList<>();
	private List<EgBillDetails> creditDetails = new ArrayList<>();
	private List<EgBillDetails> netPayableDetails = new ArrayList<>();

}
