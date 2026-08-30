package org.egov.finance.migration.modules.bank.dto;

import lombok.Data;

@Data
public class BankRecord {
   
	private Integer serialNumber;
    private String ulbName;
    private String bankName;
    private String narration;
    private int startRow;
    private int endRow;
}
