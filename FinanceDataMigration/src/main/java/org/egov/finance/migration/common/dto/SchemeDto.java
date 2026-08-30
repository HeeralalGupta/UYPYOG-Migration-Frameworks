package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class SchemeDto {

	private Long id;
	private String code;
	private String name;
	private Date validFrom;
	private Date validTo;
	private Boolean isActive;
	private String description;
	private Fund fund;
	private Date createdDate;
	private Date lastModifiedDate;
	private Long createdBy;
	private Long lastModifiedBy;
}
