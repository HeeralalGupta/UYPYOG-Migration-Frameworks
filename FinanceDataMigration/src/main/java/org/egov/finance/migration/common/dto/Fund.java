package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class Fund {
	private Long id;

	private String name;
	private String code;
	private Character identifier;
	private String llevel;
	private Fund parentId;
	private Boolean isnotleaf;
	private Boolean isactive;
	private Long createdby;
	private Date createdDate;

	private Long lastModifiedBy;
	private Date lastModifiedDate;
}
