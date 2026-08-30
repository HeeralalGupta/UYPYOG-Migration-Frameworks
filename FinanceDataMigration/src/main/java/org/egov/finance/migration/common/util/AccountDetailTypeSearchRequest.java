package org.egov.finance.migration.common.util;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDetailTypeSearchRequest {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	private String tenantId;
	private String accountDetailTypes;

}