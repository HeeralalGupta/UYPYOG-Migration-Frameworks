package org.egov.finance.migration.common.dto;

import java.sql.Date;

import lombok.Data;

@Data
public class Supplier {

    private Long id;

    private String code;

    private String name;

    private String correspondenceAddress;

    private String paymentAddress;

    private String contactPerson;

    private String email;

    private String narration;

    private String panNumber;

    private String tinNumber;

    private String ifscCode;

    private String bankAccount;

    private String source;

    private String mobileNumber;

    private String registrationNumber;

    private String epfNumber;

    private String esiNumber;

    private String gstRegisteredState;

    /*
     * Supplier type
     * Example: INDIVIDUALS
     */
    private String supplierType;

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