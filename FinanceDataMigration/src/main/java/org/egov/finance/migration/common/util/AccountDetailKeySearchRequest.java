package org.egov.finance.migration.common.util;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDetailKeySearchRequest {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;

	private String tenantId;

	private Integer accountDetailTypeId;

	private String name;

	public AccountDetailKeySearchRequest() {
	}

}