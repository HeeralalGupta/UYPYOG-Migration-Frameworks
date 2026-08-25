package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

public class ExpenseDeductionRecord {

    private String glCode;

    private String accountHead;

    private BigDecimal deductionPercentage;

    private BigDecimal creditAmount;

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

    public BigDecimal getDeductionPercentage() {
        return deductionPercentage;
    }

    public void setDeductionPercentage(
            BigDecimal deductionPercentage) {
        this.deductionPercentage = deductionPercentage;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }
}