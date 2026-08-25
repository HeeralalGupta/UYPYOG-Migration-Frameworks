package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

public class ExpenseNetPayableRecord {

    private String glCode;

    private BigDecimal creditAmount;

    public String getGlCode() {
        return glCode;
    }

    public void setGlCode(String glCode) {
        this.glCode = glCode;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }
}