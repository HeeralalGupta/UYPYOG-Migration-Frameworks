package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class BankAccount {
	
	private Long id;
	private String accountnumber;
	private String name;
	private String ifsccode;
	private String accounttype;
	private String narration;
	private Boolean isactive;
	private String payTo;
	private String type;
	private Long createdBy;
	private Date createdDate;
	private Long lastModifiedBy;
	private Date lastModifiedDate;

}
