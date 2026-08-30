package org.egov.finance.migration.modules.scheme.dto;

import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.SchemeDto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateSchemeRequest {
   
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	
	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("fundName")
	private String fundName;
	
	@JsonProperty("scheme")
	private SchemeDto schemeDto;
}
