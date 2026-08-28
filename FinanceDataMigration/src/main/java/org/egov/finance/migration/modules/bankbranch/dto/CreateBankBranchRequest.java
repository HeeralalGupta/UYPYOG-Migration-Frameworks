package org.egov.finance.migration.modules.bankbranch.dto;

import org.egov.finance.migration.common.dto.BankBranch;
import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateBankBranchRequest {

	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	
	@JsonProperty("tenantId")
	private String tenantId;
	
	@JsonProperty("bankName")
	private String bankName;
	
	@JsonProperty("bankbranch")
	private BankBranch bankbranch;
}
