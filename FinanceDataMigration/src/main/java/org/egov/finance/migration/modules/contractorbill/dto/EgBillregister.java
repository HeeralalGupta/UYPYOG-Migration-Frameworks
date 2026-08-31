package org.egov.finance.migration.modules.contractorbill.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class EgBillregister {

	private String billnumber;
	private String billdate;
	private BigDecimal billamount;
	private BigDecimal passedamount;
	private String billtype;
	private String expendituretype;
	private String workordernumber;

	private EgBillregistermis egBillregistermis;

	private List<EgBillDetails> debitDetails;
	private List<EgBillDetails> creditDetails;
	private List<EgBillDetails> netPayableDetails;

	private List<EgBillWorkItemsDTO> egBillWorkItemsDTO;
	
}