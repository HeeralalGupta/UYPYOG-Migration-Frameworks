package org.egov.finance.migration.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ChartOfAccountsSearchRequest {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;

	private String tenantId;

	private String glcode;

	
}