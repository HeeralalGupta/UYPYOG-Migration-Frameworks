package org.egov.finance.migration.modules.supplierbill.dto;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupplierBillCreateRequest {
	
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
    private String tenantId;
    private SupplierBillRequest supplierBillRequest;
}
