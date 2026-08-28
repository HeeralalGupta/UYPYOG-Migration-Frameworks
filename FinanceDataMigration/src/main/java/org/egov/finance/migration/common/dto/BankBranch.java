package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class BankBranch {

	private Integer id;
	private Bank bank;
	private String branchcode;
	private String branchname;
	private String ifscCode;
	private String branchaddress1;
	private String branchaddress2;
	private String branchcity;
	private String branchstate;
	private String branchpin;
	private String branchphone;
	private String branchfax;
	private String contactperson;
	private Boolean isactive;
	private String narration;
	private String branchMICR;
	private Long createdBy;
	private Date createdDate;
	private Long lastModifiedBy;
	private Date lastModifiedDate;
}
