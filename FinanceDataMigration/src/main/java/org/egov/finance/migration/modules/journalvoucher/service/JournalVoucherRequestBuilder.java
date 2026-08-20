package org.egov.finance.migration.modules.journalvoucher.service;

import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.util.DepartmentMapping;
import org.egov.finance.migration.common.util.FundServiceClient;
import org.egov.finance.migration.modules.journalvoucher.dto.AccountDetailContract;
import org.egov.finance.migration.modules.journalvoucher.dto.FunctionContract;
import org.egov.finance.migration.modules.journalvoucher.dto.FundContract;
import org.egov.finance.migration.modules.journalvoucher.dto.JournalVoucherLedger;
import org.egov.finance.migration.modules.journalvoucher.dto.JournalVoucherRecord;
import org.egov.finance.migration.modules.journalvoucher.dto.SchemeContract;
import org.egov.finance.migration.modules.journalvoucher.dto.SubSchemeContract;
import org.egov.finance.migration.modules.journalvoucher.dto.Voucher;
import org.egov.finance.migration.modules.journalvoucher.dto.VoucherRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JournalVoucherRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;
	private final FundServiceClient fundServiceClient;

	public JournalVoucherRequestBuilder(RequestInfoBuilder requestInfoBuilder, FundServiceClient fundServiceClient) {

		this.requestInfoBuilder = requestInfoBuilder;
		this.fundServiceClient = fundServiceClient;
	}

	/**
	 * Build Finance API VoucherRequest from one JournalVoucherRecord.
	 */
	public VoucherRequest build(JournalVoucherRecord record, MigrationRequest migrationRequest) {

		VoucherRequest request = new VoucherRequest();

		/*
		 * Tenant
		 */
		String tenantId = migrationRequest.getTenantId();
		request.setTenantId(migrationRequest.getTenantId());
		
		/*
		 * RequestInfo
		 */
		RequestInfo requestInfo = requestInfoBuilder.build();
		request.setRequestInfo(requestInfoBuilder.build());

		/*
		 * Build one Finance Voucher
		 */
		Voucher voucher = buildVoucher(record, requestInfo, tenantId);

		/*
		 * IMPORTANT: One request contains only ONE voucher.
		 *
		 * This is intentional because we want:
		 *
		 * Voucher 1 → API Voucher 2 → API Voucher 3 → API
		 */
		request.getVouchers().add(voucher);

		return request;
	}

	/**
	 * Convert our migration DTO into the Finance API Voucher DTO.
	 */
	private Voucher buildVoucher(JournalVoucherRecord record, RequestInfo requestInfo, String tenantId) {

		Voucher voucher = new Voucher();

		/*
		 * Voucher information
		 */
		voucher.setName(record.getVoucherName());
		voucher.setType(record.getVoucherType());
		voucher.setVoucherDate(record.getVoucherDate());
		String departmentName = record.getDepartment();
		String departmentCode = DepartmentMapping.getDepartmentCode(departmentName);
		voucher.setDepartment(departmentCode);
		voucher.setSource(record.getSource());
		voucher.setDescription(record.getDescription());
		voucher.setServiceName(record.getServiceName());

		/*
		 * Fund
		 */
		if (hasValue(record.getFund())) {
			Fund fundResponse = fundServiceClient.getFundByName(record.getFund(), requestInfo, tenantId);
			if (fundResponse == null) {
				throw new IllegalArgumentException("Fund not found: " + record.getFund());
			}

			if (!hasValue(fundResponse.getCode())) {
				throw new IllegalArgumentException("Fund code not found for fund: " + record.getFund());
			}
			FundContract fund = new FundContract();
			fund.setCode(fundResponse.getCode());
			voucher.setFund(fund);
		}

		/*
		 * Function
		 */
		if (hasValue(record.getFunction())) {
			FunctionContract function = new FunctionContract();
			function.setCode(record.getFunction());
			voucher.setFunction(function);
		}

		/*
		 * Scheme
		 */
		if (hasValue(record.getScheme())) {
			SchemeContract scheme = new SchemeContract();
			scheme.setCode(record.getScheme());
			voucher.setScheme(scheme);
		}

		/*
		 * Sub Scheme
		 */
		if (hasValue(record.getSubScheme())) {
			SubSchemeContract subScheme = new SubSchemeContract();
			subScheme.setCode(record.getSubScheme());
			voucher.setSubScheme(subScheme);
		}

		/*
		 * Account / Ledger details
		 */
		List<AccountDetailContract> ledgers = new ArrayList<>();

		for (JournalVoucherLedger ledgerRecord : record.getLedgers()) {
			AccountDetailContract ledger = buildLedger(ledgerRecord);
			ledgers.add(ledger);
		}

		voucher.setLedgers(ledgers);
		return voucher;
	}

	/**
	 * Build Finance AccountDetailContract.
	 */
	private AccountDetailContract buildLedger(JournalVoucherLedger ledgerRecord) {

		AccountDetailContract ledger = new AccountDetailContract();
		ledger.setGlcode(ledgerRecord.getGlCode());
		ledger.setDebitAmount(ledgerRecord.getDebitAmount());
		ledger.setCreditAmount(ledgerRecord.getCreditAmount());

		/*
		 * Subledger processing will be added in the next step.
		 *
		 * For now we preserve the information inside JournalVoucherLedger.
		 */

		return ledger;
	}

	private boolean hasValue(String value) {

		return value != null && !value.trim().isEmpty();
	}
}