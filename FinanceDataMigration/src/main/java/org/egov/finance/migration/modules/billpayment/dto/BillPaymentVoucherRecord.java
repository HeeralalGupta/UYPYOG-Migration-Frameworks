package org.egov.finance.migration.modules.billpayment.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class BillPaymentVoucherRecord {

    private int startRow;
    private int endRow;
    
    private String voucherGroupId;
    private String ulbName;
    private String paymentVoucherDate;
    private String fund;
    private String function;
    private String department;
    private String paymentMode;
    private String bankName;
    private String bankBranchName;
    private String ifscCode;
    private String bankAccountNumber;
    private String description;

    private List<PaymentBillRecord> bills = new ArrayList<>();
}
