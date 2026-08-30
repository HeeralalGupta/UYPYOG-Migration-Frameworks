package org.egov.finance.migration.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Accountdetailtype {

	private Integer version;
	private Integer id;
	private String name;
	private String description;
	private String tablename;
	private String columnname;
	private String attributename;
	private Integer nbroflevels;
	private Boolean isactive;
	private String fullQualifiedName;
	private Long createdDate;
	private Long lastModifiedDate;
	private Long lastModifiedBy;

	@JsonProperty("new")
	private Boolean isNew;

	public Accountdetailtype() {
	}
}