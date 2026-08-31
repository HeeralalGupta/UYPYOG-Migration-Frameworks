package org.egov.finance.migration.modules.contractorbill.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.ChartOfAccountsResponse;
import org.egov.finance.migration.common.dto.Function;
import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.dto.Scheme;
import org.egov.finance.migration.common.util.ChartOfAccountsServiceClient;
import org.egov.finance.migration.common.util.DepartmentMapping;
import org.egov.finance.migration.common.util.FunctionServiceClient;
import org.egov.finance.migration.common.util.FundServiceClient;
import org.egov.finance.migration.common.util.SchemeServiceClient;
import org.egov.finance.migration.common.util.WorkOrderServiceClient;
import org.egov.finance.migration.modules.contractorbill.dto.ContractorBillCreateRequest;
import org.egov.finance.migration.modules.contractorbill.dto.ContractorBillRecord;
import org.egov.finance.migration.modules.contractorbill.dto.ContractorBillRequest;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillDetails;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillPayeedetails;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillWorkItemsDTO;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillregister;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillregistermis;
import org.egov.finance.migration.modules.contractorbill.dto.IdReference;
import org.springframework.stereotype.Service;

@Service
public class ContractorBillRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;
	private final FundServiceClient fundServiceClient;
	private final FunctionServiceClient functionServiceClient;
	private final SchemeServiceClient schemeServiceClient;
	private final WorkOrderServiceClient workOrderServiceClient;
	private final ChartOfAccountsServiceClient chartOfAccountsServiceClient;

	public ContractorBillRequestBuilder(RequestInfoBuilder requestInfoBuilder, FundServiceClient fundServiceClient,
			FunctionServiceClient functionServiceClient, SchemeServiceClient schemeServiceClient,
			WorkOrderServiceClient workOrderServiceClient, ChartOfAccountsServiceClient chartOfAccountsServiceClient) {

		this.requestInfoBuilder = requestInfoBuilder;
		this.fundServiceClient = fundServiceClient;
		this.functionServiceClient = functionServiceClient;
		this.schemeServiceClient = schemeServiceClient;
		this.workOrderServiceClient = workOrderServiceClient;
		this.chartOfAccountsServiceClient = chartOfAccountsServiceClient;
	}

	/**
	 * Build Finance Contractor Bill Create Request from one Excel
	 * ContractorBillRecord.
	 */
	public ContractorBillCreateRequest build(ContractorBillRecord record, MigrationRequest migrationRequest) {

		if (record == null) {
			throw new IllegalArgumentException("Contractor Bill record cannot be null.");
		}

		if (migrationRequest == null) {
			throw new IllegalArgumentException("Migration request cannot be null.");
		}

		String tenantId = migrationRequest.getTenantId();

		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is required for Contractor Bill migration.");
		}

		/*
		 * ========================================================= ROOT REQUEST
		 * =========================================================
		 */

		ContractorBillCreateRequest request = new ContractorBillCreateRequest();

		/*
		 * RequestInfo
		 */
		RequestInfo requestInfo = requestInfoBuilder.build(tenantId);
		request.setRequestInfo(requestInfo);

		/*
		 * Tenant
		 */
		request.setTenantId(tenantId);

		/*
		 * ========================================================= CONTRACTOR BILL
		 * REQUEST =========================================================
		 */

		ContractorBillRequest billRequest = new ContractorBillRequest();

		/*
		 * Workflow
		 */
		billRequest.setWorkFlowAction("Create And Approve");
		billRequest.setApprovalPosition(0L);
		billRequest.setApprovalComment("");
		billRequest.setApprovalDesignation("");

		/*
		 * ========================================================= BILL REGISTER
		 * =========================================================
		 */

		EgBillregister billRegister = buildBillRegister(record, tenantId, requestInfo);
		billRequest.setEgBillregister(billRegister);

		/*
		 * ========================================================= WORK ORDER ITEMS
		 * =========================================================
		 *
		 * Fetch all work items using the work order number present in Excel.
		 */

		if (!hasValue(record.getWorkOrder())) {
			throw new IllegalArgumentException("Work Order number is missing for Excel rows " + record.getStartRow() + "-" + record.getEndRow());
		}

		List<EgBillWorkItemsDTO> workItems = workOrderServiceClient.getWorkItemsByOrderNumber(record.getWorkOrder(),
				requestInfo, tenantId);

		if (workItems == null) {
			workItems = new ArrayList<>();
		}

		billRequest.setWorkItemsForBillRegister(workItems);

		/*
		 * ========================================================= FINAL REQUEST
		 * =========================================================
		 */

		request.setContractorBillRequest(billRequest);

		return request;
	}

	/**
	 * Build EgBillregister.
	 */
	private EgBillregister buildBillRegister(ContractorBillRecord record, String tenantId, RequestInfo requestInfo) {

		EgBillregister bill = new EgBillregister();

		/*
		 * ========================================================= BILL NUMBER
		 * =========================================================
		 *
		 * Current Excel does not contain billnumber.
		 */

		bill.setBillnumber(generateBillNumber(record));

		/*
		 * ========================================================= BILL DATE
		 * =========================================================
		 */

		if (!hasValue(record.getBillDate())) {
			
			throw new IllegalArgumentException(
					"Bill date is missing for Excel rows " + record.getStartRow() + "-" + record.getEndRow());
		}

		bill.setBilldate(record.getBillDate());

		/*
		 * ========================================================= BILL TYPE
		 * =========================================================
		 */

		bill.setBilltype(record.getBillType());

		/*
		 * ========================================================= BILL AMOUNT
		 * =========================================================
		 */

		if (record.getPartyBillAmount() == null) {

			throw new IllegalArgumentException(
					"Party Bill Amount is missing for Excel rows " + record.getStartRow() + "-" + record.getEndRow());
		}

		bill.setBillamount(record.getPartyBillAmount());

		/*
		 * Current Excel does not contain separate passed amount. Therefore use party
		 * bill amount.
		 */
		bill.setPassedamount(record.getPartyBillAmount());

		/*
		 * ========================================================= 
		 * WORK ORDER
		 * =========================================================
		 */

		bill.setWorkordernumber(record.getWorkOrder());

		/*
		 * ========================================================= 
		 * EXPENDITURE TYPE
		 * =========================================================
		 */

		bill.setExpendituretype("Works");

		/*
		 * ========================================================= 
		 * BILL MIS
		 * =========================================================
		 */

		EgBillregistermis mis = buildMIS(record, tenantId, requestInfo);

		bill.setEgBillregistermis(mis);

		/*
		 * ========================================================= 
		 * DEBIT DETAILS
		 * =========================================================
		 *
		 * Excel gives GL CODE. Finance API requires GL ID.
		 */
		bill.setDebitDetails(buildDebitDetails(record.getDebitDetails(), requestInfo, tenantId));

		/*
		 * ========================================================= 
		 * CREDIT DETAILS
		 * =========================================================
		 */
		bill.setCreditDetails(buildCreditDetails(record.getCreditDetails(), requestInfo, tenantId));

		/*
		 * ========================================================= 
		 * NET PAYABLE DETAILS
		 * =========================================================
		 */
		bill.setNetPayableDetails(buildNetPayableDetails(record.getNetPayableDetails(), requestInfo, tenantId));

		/*
		 * ========================================================= PAYEE DETAILS
		 * =========================================================
		 *
		 * No payee/subledger data is currently read from Excel.
		 */

//		bill.setBillPayeedetails(new ArrayList<EgBillPayeedetails>());

		return bill;
	}

	/**
	 * Build MIS information.
	 */
	private EgBillregistermis buildMIS(ContractorBillRecord record, String tenantId, RequestInfo requestInfo) {

		EgBillregistermis mis = new EgBillregistermis();

		/*
		 * ========================================================= FUND
		 * =========================================================
		 *
		 * Excel: Municipal Fund
		 *
		 * Finance: { "id": 1 }
		 */

		if (hasValue(record.getFund())) {

			Fund fundResponse = fundServiceClient.getFundByName(record.getFund(), requestInfo, tenantId);

			if (fundResponse == null) {
				throw new IllegalArgumentException("Fund not found: " + record.getFund() + " for Excel rows "
						+ record.getStartRow() + "-" + record.getEndRow());
			}

			if (fundResponse.getId() == null) {
				throw new IllegalArgumentException("Fund ID not found for fund: " + record.getFund());
			}

			IdReference fund = new IdReference();
			fund.setId(fundResponse.getId());
			mis.setFund(fund);
		}

		/*
		 * ========================================================= DEPARTMENT
		 * =========================================================
		 */

		if (hasValue(record.getDepartment())) {

			String departmentCode = DepartmentMapping.getDepartmentCode(record.getDepartment());

			if (!hasValue(departmentCode)) {
				throw new IllegalArgumentException("Department Code not found for department: " + record.getDepartment());
			}
			mis.setDepartmentcode(departmentCode);
		}

		/*
		 * ========================================================= FUNCTION
		 * =========================================================
		 *
		 * Excel: General Administration
		 *
		 * Finance: { "id": 1 }
		 */

		if (hasValue(record.getFunction())) {

			Function functionResponse = functionServiceClient.getFunctionByName(record.getFunction(), requestInfo,
					tenantId);

			if (functionResponse == null || functionResponse.getId() == null) {
				throw new IllegalArgumentException("Function ID not found for function: " + record.getFunction());
			}

			IdReference function = new IdReference();
			function.setId(functionResponse.getId());
			mis.setFunction(function);
		}

		/*
		 * ========================================================= SCHEME
		 * =========================================================
		 *
		 * Excel: AMRUT
		 *
		 * Finance: schemeId = 2
		 */

		if (hasValue(record.getScheme())) {

			if (!hasValue(record.getFund())) {
				throw new IllegalArgumentException("Fund is required to search Scheme: " + record.getScheme());
			}

			Scheme schemeResponse = schemeServiceClient.getSchemeByName(record.getScheme(), record.getFund(),
					requestInfo, tenantId);

			if (schemeResponse == null || schemeResponse.getId() == null) {
				throw new IllegalArgumentException(
						"Scheme ID not found for scheme: " + record.getScheme() + ", Fund: " + record.getFund());
			}

			mis.setSchemeId(schemeResponse.getId());
		}

		/*
		 * ========================================================= SUB SCHEME
		 * =========================================================
		 *
		 * Not mapped yet because the current Excel reader does not provide a SubScheme
		 * value/API lookup.
		 */

		/*
		 * ========================================================= NARRATION
		 * =========================================================
		 */

		mis.setNarration(record.getNarration());

		/*
		 * ========================================================= PARTY BILL NUMBER
		 * =========================================================
		 */

		mis.setPartyBillNumber(record.getPartyBillNo());

		/*
		 * ========================================================= PARTY BILL DATE
		 * =========================================================
		 */

		mis.setPartyBillDate(record.getPartyBillDate());

		/*
		 * ========================================================= FUND SOURCE
		 * =========================================================
		 *
		 * Not resolved yet because its search API has not been provided.
		 */

		return mis;
	}

	/**
	 * ============================================================= DEBIT DETAILS
	 * =============================================================
	 *
	 * Excel value: 3501000001
	 *
	 * API value: 786
	 *
	 * GL Code -> Chart Of Accounts -> ID
	 */
	private List<EgBillDetails> buildDebitDetails(List<EgBillDetails> source, RequestInfo requestInfo,
			String tenantId) {

		List<EgBillDetails> result = new ArrayList<>();

		if (source == null) {
			return result;
		}

		for (EgBillDetails sourceDetail : source) {

			if (sourceDetail == null || sourceDetail.getGlcodeid() == null) {
				continue;
			}

			String glCode = sourceDetail.getGlcodeid().toPlainString();

			ChartOfAccountsResponse account = chartOfAccountsServiceClient.getByGlCode(glCode, requestInfo, tenantId);

			if (account == null || account.getId() == null) {
				throw new IllegalArgumentException("GL Code ID not found for debit GL Code: " + glCode);
			}

			EgBillDetails detail = new EgBillDetails();

			detail.setGlcodeid(BigDecimal.valueOf(account.getId()));
			detail.setDebitamount(sourceDetail.getDebitamount());
			detail.setFunctionid(sourceDetail.getFunctionid());
			detail.setNarration(sourceDetail.getNarration());

			result.add(detail);
		}

		return result;
	}

	/**
	 * ============================================================= CREDIT DETAILS
	 * =============================================================
	 */
	private List<EgBillDetails> buildCreditDetails(List<EgBillDetails> source, RequestInfo requestInfo,
			String tenantId) {

		List<EgBillDetails> result = new ArrayList<>();

		if (source == null) {
			return result;
		}

		for (EgBillDetails sourceDetail : source) {

			if (sourceDetail == null || sourceDetail.getGlcodeid() == null) {

				continue;
			}

			String glCode = sourceDetail.getGlcodeid().toPlainString();

			ChartOfAccountsResponse account = chartOfAccountsServiceClient.getByGlCode(glCode, requestInfo, tenantId);

			if (account == null || account.getId() == null) {

				throw new IllegalArgumentException("GL Code ID not found for credit GL Code: " + glCode);
			}

			EgBillDetails detail = new EgBillDetails();

			detail.setGlcodeid(BigDecimal.valueOf(account.getId()));
			detail.setCreditamount(sourceDetail.getCreditamount());
			detail.setFunctionid(sourceDetail.getFunctionid());
			detail.setNarration(sourceDetail.getNarration());

			result.add(detail);
		}

		return result;
	}

	/**
	 * ============================================================= NET PAYABLE
	 * DETAILS =============================================================
	 *
	 * Finance API expects net payable amount in creditamount.
	 */
	private List<EgBillDetails> buildNetPayableDetails(List<EgBillDetails> source, RequestInfo requestInfo,
			String tenantId) {

		List<EgBillDetails> result = new ArrayList<>();

		if (source == null) {
			return result;
		}

		for (EgBillDetails sourceDetail : source) {

			if (sourceDetail == null || sourceDetail.getGlcodeid() == null) {
				continue;
			}

			String glCode = sourceDetail.getGlcodeid().toPlainString();

			ChartOfAccountsResponse account = chartOfAccountsServiceClient.getByGlCode(glCode, requestInfo, tenantId);

			if (account == null || account.getId() == null) {
				throw new IllegalArgumentException("GL Code ID not found for net payable GL Code: " + glCode);
			}

			EgBillDetails detail = new EgBillDetails();

			detail.setGlcodeid(BigDecimal.valueOf(account.getId()));
			detail.setCreditamount(sourceDetail.getCreditamount());
			detail.setFunctionid(sourceDetail.getFunctionid());
			detail.setNarration(sourceDetail.getNarration());

			result.add(detail);
		}

		return result;
	}

	/**
	 * Generate migration bill number.
	 *
	 * Example: CON-BILL-MIG-00005
	 */
	private String generateBillNumber(ContractorBillRecord record) {
		return String.format("CON-BILL-MIG-%05d", record.getStartRow());
	}

	private boolean hasValue(String value) {
		return value != null && !value.trim().isEmpty();
	}
}