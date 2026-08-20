package org.egov.finance.migration.modules.journalvoucher.dto;


import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class JournalVoucherRecord {

    /*
     * Excel tracking
     */
    private int startRow;
    private int endRow;

    /*
     * Voucher information
     */
    private String ulbName;
    private String voucherDate;
    private String voucherName;
    private String voucherType;
    private String department;
    private String departmentOther;
    private String fund;
    private String fundOther;
    private String function;
    private String scheme;
    private String subScheme;
    private String source;
    private String description;
    private String serviceName;

    /*
     * Account details
     */
    private List<JournalVoucherLedger> ledgers =
            new ArrayList<>();
}