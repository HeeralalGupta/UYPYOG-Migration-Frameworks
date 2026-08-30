package org.egov.finance.migration.modules.expensebill.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseBillRequest {

	
	private String workFlowAction;
	private Long approvalPosition;
	private String approvalComment;
	private String approvalDesignation;
	private EgBillregister egBillregister;

}