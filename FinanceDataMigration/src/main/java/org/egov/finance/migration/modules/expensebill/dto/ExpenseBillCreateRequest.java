package org.egov.finance.migration.modules.expensebill.dto;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseBillCreateRequest {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
    private String tenantId;
    private ExpenseBillRequest expenseBillRequest;

    
}