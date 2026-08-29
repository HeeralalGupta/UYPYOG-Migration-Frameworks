package org.egov.finance.migration.modules.bankaccount.service;

import org.egov.finance.migration.common.dto.BankAccount;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.bankaccount.dto.BankAccountRecord;
import org.egov.finance.migration.modules.bankaccount.dto.CreateBankAccountRequest;
import org.springframework.stereotype.Service;

@Service
public class BankAccountRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;

	public BankAccountRequestBuilder(RequestInfoBuilder requestInfoBuilder) {

		this.requestInfoBuilder = requestInfoBuilder;
	}

	/**
	 * Build Finance API BankAccountRequest from one BankAccountRecord.
	 */
	public CreateBankAccountRequest build(BankAccountRecord record, MigrationRequest migrationRequest) {

		CreateBankAccountRequest request = new CreateBankAccountRequest();

		/*
		 * Tenant
		 */
		String tenantId = migrationRequest.getTenantId();

		request.setTenantId(tenantId);

		/*
		 * RequestInfo
		 */
		RequestInfo requestInfo = requestInfoBuilder.build(tenantId);

		request.setRequestInfo(requestInfo);

		/*
		 * Build one Finance Bank Account
		 */
		BankAccount bankAccount = buildBankAccount(record);
		request.setBranchName(record.getBranchName());
		request.setFundName(record.getFundName());
		request.setBankaccount(bankAccount);

		return request;
	}

	/**
	 * Convert migration BankAccountRecord into Finance API BankAccount DTO.
	 */
	private BankAccount buildBankAccount(BankAccountRecord record) {

		BankAccount bankAccount = new BankAccount();

		/*
		 * Bank Account information
		 */
		
		bankAccount.setAccountnumber(record.getAccountNumber());
		bankAccount.setIfsccode(record.getIfscCode());
		bankAccount.setAccounttype(record.getAccountType());
		bankAccount.setNarration(record.getDescription());
		bankAccount.setPayTo(record.getPayTo());
        bankAccount.setType(record.getType());
		/*
		 * Default values
		 */
		bankAccount.setIsactive(true);
		return bankAccount;
	}
}