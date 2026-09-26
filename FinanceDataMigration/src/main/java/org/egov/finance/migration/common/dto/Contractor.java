package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
public class Contractor {
	
    private Long id;
    private String code;
    private String name;

    private String correspondenceAddress;
    private String paymentAddress;

    private String contactPerson;
    private String email;
    private String narration;

    private String panNumber;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String tinNumber;
    private String ifscCode;
    private String bankAccount;
    private String source;
    private String mobileNumber;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String registrationNumber;
    private String epfNumber;
    private String esiNumber;

    private String gstRegisteredState;
    /*
     * Contractor type
     * Example: INDIVIDUALS
     */
    private String contractorType;
    private String gstReason;
    private String panReason;

    /*
     * Audit fields
     */
    private Long createdBy;
    private Date createdDate;

    private Long lastModifiedBy;
    private Date lastModifiedDate;
}
