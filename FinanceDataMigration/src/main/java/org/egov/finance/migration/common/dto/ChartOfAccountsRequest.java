package org.egov.finance.migration.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChartOfAccountsRequest {

	private RequestInfo requestInfo;

	private String tenantId;

	private String glcode;

}