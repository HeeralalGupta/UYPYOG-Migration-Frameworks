package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class Bank {
	
	    private Integer id;
	    private String code;
	    private String name;
	    private String narration;
	    private Boolean isactive;
	    private String type;
	    private Long createdBy;
	    private Date createdDate;
	    private Long lastModifiedBy;
	    private Date lastModifiedDate;
}
