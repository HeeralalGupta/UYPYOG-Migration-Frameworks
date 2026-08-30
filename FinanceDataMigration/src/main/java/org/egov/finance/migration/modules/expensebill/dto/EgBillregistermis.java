package org.egov.finance.migration.modules.expensebill.dto;

public class EgBillregistermis {

	private IdDTO fund;
	private Long schemeId;
	private Long subSchemeId;
	private IdDTO function;
	private String fundsource;
	private String departmentcode;
	private String narration;
	private String partyBillNumber;
	private String partyBillDate;
	private String payto;
	private IdDTO egBillSubType;

	public IdDTO getFund() {
		return fund;
	}

	public void setFund(IdDTO fund) {
		this.fund = fund;
	}

	public Long getSchemeId() {
		return schemeId;
	}

	public void setSchemeId(Long schemeId) {
		this.schemeId = schemeId;
	}

	public Long getSubSchemeId() {
		return subSchemeId;
	}

	public void setSubSchemeId(Long subSchemeId) {
		this.subSchemeId = subSchemeId;
	}

	public IdDTO getFunction() {
		return function;
	}

	public void setFunction(IdDTO function) {
		this.function = function;
	}

	public String getFundsource() {
		return fundsource;
	}

	public void setFundsource(String fundsource) {
		this.fundsource = fundsource;
	}

	public String getDepartmentcode() {
		return departmentcode;
	}

	public void setDepartmentcode(String departmentcode) {
		this.departmentcode = departmentcode;
	}

	public String getNarration() {
		return narration;
	}

	public void setNarration(String narration) {
		this.narration = narration;
	}

	public String getPartyBillNumber() {
		return partyBillNumber;
	}

	public void setPartyBillNumber(String partyBillNumber) {
		this.partyBillNumber = partyBillNumber;
	}

	public String getPartyBillDate() {
		return partyBillDate;
	}

	public void setPartyBillDate(String partyBillDate) {
		this.partyBillDate = partyBillDate;
	}

	public String getPayto() {
		return payto;
	}

	public void setPayto(String payto) {
		this.payto = payto;
	}

	public IdDTO getEgBillSubType() {
		return egBillSubType;
	}

	public void setEgBillSubType(IdDTO egBillSubType) {
		this.egBillSubType = egBillSubType;
	}
}