package org.egov.finance.migration.modules.contractorbill.dto;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ContractorBillCreateRequest {
	
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
    private String tenantId;
    private ContractorBillRequest contractorBillRequest;
}
