package org.egov.finance.migration.common.dto;

import lombok.Data;

@Data
public class Function {
	private Long id;
	private String name;
	private String code;
	private Integer level;
	private Boolean active;
	private Long parentId;
	private AuditDetails auditDetails;
}
