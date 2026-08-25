package org.egov.finance.migration.modules.expensebill.dto;

import java.util.ArrayList;
import java.util.List;

public class ExpenseBillRecord {

    private Integer serialNumber;

    // Bill Details
    private String ulbName;
    private String billDate;
    private String fund;
    private String department;
    private String scheme;
    private String fundSource;
    private String function;
    private String narration;
    private String partyBillNo;
    private String partyBillDate;
    private String billSubType;

    // SubLedger Details
    private String subLedgerType;
    private String subLedgerMaster;

    // Transaction Details
    private List<ExpenseDebitRecord> debitDetails = new ArrayList<>();
    private List<ExpenseDeductionRecord> deductionDetails = new ArrayList<>();
    private ExpenseNetPayableRecord netPayableDetail;

    public Integer getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(Integer serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getUlbName() {
        return ulbName;
    }

    public void setUlbName(String ulbName) {
        this.ulbName = ulbName;
    }

    public String getBillDate() {
        return billDate;
    }

    public void setBillDate(String billDate) {
        this.billDate = billDate;
    }

    public String getFund() {
        return fund;
    }

    public void setFund(String fund) {
        this.fund = fund;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getFundSource() {
        return fundSource;
    }

    public void setFundSource(String fundSource) {
        this.fundSource = fundSource;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getNarration() {
        return narration;
    }

    public void setNarration(String narration) {
        this.narration = narration;
    }

    public String getPartyBillNo() {
        return partyBillNo;
    }

    public void setPartyBillNo(String partyBillNo) {
        this.partyBillNo = partyBillNo;
    }

    public String getPartyBillDate() {
        return partyBillDate;
    }

    public void setPartyBillDate(String partyBillDate) {
        this.partyBillDate = partyBillDate;
    }

    public String getBillSubType() {
        return billSubType;
    }

    public void setBillSubType(String billSubType) {
        this.billSubType = billSubType;
    }

    public String getSubLedgerType() {
        return subLedgerType;
    }

    public void setSubLedgerType(String subLedgerType) {
        this.subLedgerType = subLedgerType;
    }

    public String getSubLedgerMaster() {
        return subLedgerMaster;
    }

    public void setSubLedgerMaster(String subLedgerMaster) {
        this.subLedgerMaster = subLedgerMaster;
    }

    public List<ExpenseDebitRecord> getDebitDetails() {
        return debitDetails;
    }

    public void setDebitDetails(List<ExpenseDebitRecord> debitDetails) {
        this.debitDetails = debitDetails;
    }

    public List<ExpenseDeductionRecord> getDeductionDetails() {
        return deductionDetails;
    }

    public void setDeductionDetails(
            List<ExpenseDeductionRecord> deductionDetails) {
        this.deductionDetails = deductionDetails;
    }

    public ExpenseNetPayableRecord getNetPayableDetail() {
        return netPayableDetail;
    }

    public void setNetPayableDetail(
            ExpenseNetPayableRecord netPayableDetail) {
        this.netPayableDetail = netPayableDetail;
    }
}