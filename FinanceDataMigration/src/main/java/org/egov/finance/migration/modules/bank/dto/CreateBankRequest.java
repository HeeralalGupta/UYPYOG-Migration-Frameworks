package org.egov.finance.migration.modules.bank.dto;

import org.egov.finance.migration.common.dto.Bank;
import org.egov.finance.migration.common.dto.RequestInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CreateBankRequest {
   
	@JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("bank")
    private Bank bank;
}
