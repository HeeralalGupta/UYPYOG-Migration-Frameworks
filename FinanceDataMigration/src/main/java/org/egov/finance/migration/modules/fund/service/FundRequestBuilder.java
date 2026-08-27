package org.egov.finance.migration.modules.fund.service;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.FundRequest;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.fund.dto.CreateFundRequest;
import org.egov.finance.migration.modules.fund.dto.FundRecord;
import org.springframework.stereotype.Service;

@Service
public class FundRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;

	public FundRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
		this.requestInfoBuilder = requestInfoBuilder;
	}

	/**
	 * Build Finance API FundRequest from one Fund record.
	 */
	public CreateFundRequest build(FundRecord record, MigrationRequest migrationRequest) {

		CreateFundRequest request = new CreateFundRequest();

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
		 * Build one Finance Fund
		 */
		Fund fund = buildFund(record);

		/*
		 * Add fund to request
		 */
		request.setFund(fund);

		return request;
	}

	/**
	 * Convert migration FundRecord into Finance API Fund DTO.
	 */
	private Fund buildFund(FundRecord record) {

		Fund fund = new Fund();

		/*
		 * Fund information
		 */
		fund.setLlevel(record.getNatureOfFund());
		fund.setName(record.getFundName());
		fund.setIsactive(true);
		fund.setIdentifier('1');
		fund.setIsnotleaf(false);

		return fund;
	}
}