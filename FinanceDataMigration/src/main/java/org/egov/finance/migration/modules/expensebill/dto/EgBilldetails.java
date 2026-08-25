package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;

public class EgBilldetails {

	private Long glcodeid;
	private BigDecimal debitamount;
	private BigDecimal creditamount;

	public Long getGlcodeid() {
		return glcodeid;
	}

	public void setGlcodeid(Long glcodeid) {
		this.glcodeid = glcodeid;
	}

	public BigDecimal getDebitamount() {
		return debitamount;
	}

	public void setDebitamount(BigDecimal debitamount) {
		this.debitamount = debitamount;
	}

	public BigDecimal getCreditamount() {
		return creditamount;
	}

	public void setCreditamount(BigDecimal creditamount) {
		this.creditamount = creditamount;
	}

}