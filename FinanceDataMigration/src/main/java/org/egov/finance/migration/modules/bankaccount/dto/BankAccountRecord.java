package org.egov.finance.migration.modules.bankaccount.dto;

import lombok.Data;

@Data
public class BankAccountRecord {
		
	    private Integer serialNumber;
	    private String ulbName;
	    private String branchName;
	    private String ifscCode;
	    private String accountNumber;
	    private String fundName;
	    private String accountType;
	    private String description;
	    private String payTo;
	    private String type;
	    private int startRow;
	    private int endRow;
}
