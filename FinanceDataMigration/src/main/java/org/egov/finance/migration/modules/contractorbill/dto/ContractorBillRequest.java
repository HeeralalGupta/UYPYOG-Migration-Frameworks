package org.egov.finance.migration.modules.contractorbill.dto;

import java.util.List;

import lombok.Data;

@Data
public class ContractorBillRequest {
	
	private String workFlowAction;
	private Long approvalPosition = 0L;
	private String approvalComment;
	private String approvalDesignation;
	private EgBillregister egBillregister;
	private List<EgBillWorkItemsDTO> workItemsForBillRegister;
}
