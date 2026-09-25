package org.egov.finance.migration.modules.expensebill.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EgBillregister {

	private BigDecimal billamount;
	private String billnumber;
	private String billdate;
	private String expendituretype;
	private EgBillregistermis egBillregistermis;
	private List<EgBilldetails> billDetails;
	private List<EgBillPayeedetails> billPayeedetails;
	private List<EgBillChecklist> checkLists;

	
}