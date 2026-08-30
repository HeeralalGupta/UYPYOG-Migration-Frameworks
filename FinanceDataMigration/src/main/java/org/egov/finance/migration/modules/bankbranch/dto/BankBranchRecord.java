package org.egov.finance.migration.modules.bankbranch.dto;

import lombok.Data;

@Data
public class BankBranchRecord {

	
	private Integer serialNumber;
    private String ulbName;
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String branchCode;
    private String micr;
    private String address;
    private String contactPerson;
    private String phoneNumber;
    private String narration;

    private int startRow;
    private int endRow;
}
