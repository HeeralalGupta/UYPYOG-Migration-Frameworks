package org.egov.finance.migration.modules.bankaccount.dto;

import org.egov.finance.migration.common.dto.BankAccount;
import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateBankAccountRequest {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	
	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("branchName")
	private String branchName;
	
	@JsonProperty("fundName")
	private String fundName;
	
	@JsonProperty("bankaccount")
	private BankAccount bankaccount;
}
