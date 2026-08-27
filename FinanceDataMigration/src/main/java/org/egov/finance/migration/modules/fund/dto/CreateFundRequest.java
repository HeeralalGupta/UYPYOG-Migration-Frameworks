package org.egov.finance.migration.modules.fund.dto;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateFundRequest {
	
	@JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("fund")
    private Fund fund;
}
