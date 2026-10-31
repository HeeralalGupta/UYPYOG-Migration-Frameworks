package org.egov.finance.migration.modules.deductionpayment.dto;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateDeductionPaymentRequest {
	@JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @JsonProperty("tenantId")
    private String tenantId;
    private DeductionRemittanceRequest deductionRemittanceRequest ;
}
