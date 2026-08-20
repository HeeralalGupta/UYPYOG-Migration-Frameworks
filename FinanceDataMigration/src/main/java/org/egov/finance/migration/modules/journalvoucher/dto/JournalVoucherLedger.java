package org.egov.finance.migration.modules.journalvoucher.dto;


import lombok.Data;

@Data
public class JournalVoucherLedger {

    private String glCode;

    private Double debitAmount;

    private Double creditAmount;

    /*
     * Subledger details
     */
    private String ledgerFunctionCode;

    private String detailType;

    private String detailKey;

    private Double subledgerAmount;
}
