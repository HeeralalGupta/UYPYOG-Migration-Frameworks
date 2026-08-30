package org.egov.finance.migration.modules.contractor.dto;

import org.egov.finance.migration.common.dto.Contractor;
import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateContractorRequest {
    
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	
	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("bankName")
	private String bankName;
	
	@JsonProperty("branchName")
	private String branchName;
	
	@JsonProperty("statusId")
	private Integer statusId;
	
	@JsonProperty("contractor")
	private Contractor contractor;
}
