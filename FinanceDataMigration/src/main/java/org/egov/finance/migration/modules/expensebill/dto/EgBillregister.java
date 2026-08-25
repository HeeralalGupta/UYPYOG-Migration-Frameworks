package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;
import java.util.List;

public class EgBillregister {

	private BigDecimal billamount;
	private String billnumber;
	private String billdate;
	private String expendituretype;
	private EgBillregistermis egBillregistermis;
	private List<EgBilldetails> billDetails;
	private List<EgBillPayeedetails> billPayeedetails;
	private List<EgBillChecklist> checkLists;

	public BigDecimal getBillamount() {
		return billamount;
	}

	public void setBillamount(BigDecimal billamount) {
		this.billamount = billamount;
	}

	public String getBillnumber() {
		return billnumber;
	}

	public void setBillnumber(String billnumber) {
		this.billnumber = billnumber;
	}

	public String getBilldate() {
		return billdate;
	}

	public void setBilldate(String billdate) {
		this.billdate = billdate;
	}

	public String getExpendituretype() {
		return expendituretype;
	}

	public void setExpendituretype(String expendituretype) {
		this.expendituretype = expendituretype;
	}

	public EgBillregistermis getEgBillregistermis() {
		return egBillregistermis;
	}

	public void setEgBillregistermis(EgBillregistermis egBillregistermis) {
		this.egBillregistermis = egBillregistermis;
	}

	public List<EgBilldetails> getBillDetails() {
		return billDetails;
	}

	public void setBillDetails(List<EgBilldetails> billDetails) {
		this.billDetails = billDetails;
	}

	public List<EgBillPayeedetails> getBillPayeedetails() {
		return billPayeedetails;
	}

	public void setBillPayeedetails(List<EgBillPayeedetails> billPayeedetails) {
		this.billPayeedetails = billPayeedetails;
	}

	public List<EgBillChecklist> getCheckLists() {
		return checkLists;
	}

	public void setCheckLists(List<EgBillChecklist> checkLists) {
		this.checkLists = checkLists;
	}

}