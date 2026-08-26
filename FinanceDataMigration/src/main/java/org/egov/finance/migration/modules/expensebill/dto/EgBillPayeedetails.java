package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

public class EgBillPayeedetails {

	private Long egBilldetailsId;
	private BigDecimal debitAmount;
	private BigDecimal creditAmount;
	private Boolean isDebit;
	private Long accountDetailTypeId;
	private Long accountDetailKeyId;

	public Long getEgBilldetailsId() {
		return egBilldetailsId;
	}

	public void setEgBilldetailsId(Long egBilldetailsId) {
		this.egBilldetailsId = egBilldetailsId;
	}

	public BigDecimal getDebitAmount() {
		return debitAmount;
	}

	public void setDebitAmount(BigDecimal debitAmount) {
		this.debitAmount = debitAmount;
	}

	public BigDecimal getCreditAmount() {
		return creditAmount;
	}

	public void setCreditAmount(BigDecimal creditAmount) {
		this.creditAmount = creditAmount;
	}

	public Boolean getIsDebit() {
		return isDebit;
	}

	public void setIsDebit(Boolean debit) {
		isDebit = debit;
	}

	public Long getAccountDetailTypeId() {
		return accountDetailTypeId;
	}

	public void setAccountDetailTypeId(Long accountDetailTypeId) {
		this.accountDetailTypeId = accountDetailTypeId;
	}

	public Long getAccountDetailKeyId() {
		return accountDetailKeyId;
	}

	public void setAccountDetailKeyId(Long accountDetailKeyId) {
		this.accountDetailKeyId = accountDetailKeyId;
	}
}