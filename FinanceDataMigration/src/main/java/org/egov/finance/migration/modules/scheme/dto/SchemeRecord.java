package org.egov.finance.migration.modules.scheme.dto;

import lombok.Data;

@Data
public class SchemeRecord {

	private Integer serialNumber;
	private String ulbName;
	private String fundName;
	private String schemeCode;
	private String schemeName;
	private String validFrom;
	private String validTo;
	private String isActive;
	private String description;
	private int startRow;
	private int endRow;
}
