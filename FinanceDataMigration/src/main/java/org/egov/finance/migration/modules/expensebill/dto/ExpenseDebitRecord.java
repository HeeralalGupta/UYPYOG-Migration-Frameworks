package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

public class ExpenseDebitRecord {

    private String glCode;

    private String accountHead;

    private BigDecimal debitAmount;

    public String getGlCode() {
        return glCode;
    }

    public void setGlCode(String glCode) {
        this.glCode = glCode;
    }

    public String getAccountHead() {
        return accountHead;
    }

    public void setAccountHead(String accountHead) {
        this.accountHead = accountHead;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }
}