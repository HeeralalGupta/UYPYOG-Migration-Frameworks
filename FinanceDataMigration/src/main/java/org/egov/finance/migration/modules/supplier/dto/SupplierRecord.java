package org.egov.finance.migration.modules.supplier.dto;

import lombok.Data;

@Data
public class SupplierRecord {

    private Integer serialNumber;

    private String ulbName;

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

    private String supplierType;

    private String gstReason;

    private String panReason;

    private String bankName;

    private String branchName;

    private String status;

    private int startRow;

    private int endRow;
}