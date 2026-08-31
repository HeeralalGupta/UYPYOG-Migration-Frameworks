package org.egov.finance.migration.modules.contractorbill.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ContractorBillRecord {

	/*
	 * Excel tracking
	 */
	private int startRow;
	private int endRow;
	
	/*
	 * Main Bill Information
	 */
	private String ulbName;
	private String billDate;
	private String contractor;
	private String workOrder;
	private String fund;
	private String department;
	private String scheme;
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
