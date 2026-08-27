package org.egov.finance.migration.modules.fund.dto;

import lombok.Data;

@Data
public class FundRecord {
	
    private Integer serialNumber;
    private String ulbName;
    private String fundName;
    private String natureOfFund;
    private String parentFund;
    private int startRow;
    private int endRow;
}
