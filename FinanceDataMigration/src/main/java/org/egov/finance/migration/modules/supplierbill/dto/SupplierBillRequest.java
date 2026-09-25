package org.egov.finance.migration.modules.supplierbill.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierBillRequest {

	private String workFlowAction;
	private Long approvalPosition = 0L;
	private String approvalComment;
	private String approvalDesignation;
	private EgBillregister egBillregister;
	private List<EgBillPurchaseItemsDTO> purchaseItemsForBillRegister;
}
