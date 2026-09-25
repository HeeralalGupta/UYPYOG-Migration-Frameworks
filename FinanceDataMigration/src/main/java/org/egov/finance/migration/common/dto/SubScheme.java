package org.egov.finance.migration.common.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubScheme {
	
	private Long id;
	private String code;
	private String name;
	private Date validFrom;
	private Date validTo;
	private Boolean isActive;
	private Scheme schemeId;
	private String department;

}
