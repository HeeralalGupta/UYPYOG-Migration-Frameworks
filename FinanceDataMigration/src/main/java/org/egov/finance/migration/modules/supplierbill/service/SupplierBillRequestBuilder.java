package org.egov.finance.migration.modules.supplierbill.service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.egov.finance.migration.common.dto.ChartOfAccountsResponse;
import org.egov.finance.migration.common.dto.Function;
import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.dto.Scheme;
import org.egov.finance.migration.common.dto.SubScheme;
import org.egov.finance.migration.common.util.ChartOfAccountsServiceClient;
import org.egov.finance.migration.common.util.DepartmentMapping;
import org.egov.finance.migration.common.util.FunctionServiceClient;
import org.egov.finance.migration.common.util.FundServiceClient;
import org.egov.finance.migration.common.util.PurchaseOrderServiceClient;
import org.egov.finance.migration.common.util.SchemeServiceClient;
import org.egov.finance.migration.common.util.SubSchemeServiceClient;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRecord;
import org.egov.finance.migration.modules.supplierbill.dto.EgBillDetails;
import org.egov.finance.migration.modules.supplierbill.dto.EgBillPurchaseItemsDTO;
import org.egov.finance.migration.modules.supplierbill.dto.EgBillregister;
import org.egov.finance.migration.modules.supplierbill.dto.EgBillregistermis;
import org.egov.finance.migration.modules.supplierbill.dto.IdReference;
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillCreateRequest;
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillRecord;
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillRequest;
import org.springframework.stereotype.Service;

@Service
public class SupplierBillRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;
	private final FundServiceClient fundServiceClient;
	private final FunctionServiceClient functionServiceClient;
	private final SchemeServiceClient schemeServiceClient;
	private final PurchaseOrderServiceClient purchaseOrderServiceClient;
	private final ChartOfAccountsServiceClient chartOfAccountsServiceClient;
	private final SubSchemeServiceClient subSchemeServiceClient;

	public SupplierBillRequestBuilder(RequestInfoBuilder requestInfoBuilder, FundServiceClient fundServiceClient,
			FunctionServiceClient functionServiceClient, SchemeServiceClient schemeServiceClient,
			PurchaseOrderServiceClient purchaseOrderServiceClient,
			ChartOfAccountsServiceClient chartOfAccountsServiceClient, SubSchemeServiceClient subSchemeServiceClient) {

		this.requestInfoBuilder = requestInfoBuilder;
		this.fundServiceClient = fundServiceClient;
		this.functionServiceClient = functionServiceClient;
		this.schemeServiceClient = schemeServiceClient;
		this.purchaseOrderServiceClient = purchaseOrderServiceClient;
		this.chartOfAccountsServiceClient = chartOfAccountsServiceClient;
		this.subSchemeServiceClient = subSchemeServiceClient;
	}

	/**
	 * Build Finance Supplier Bill Create Request from one Excel SupplierBillRecord.
	 */
	public SupplierBillCreateRequest build(SupplierBillRecord record, MigrationRequest migrationRequest) {

		if (record == null) {
			throw new IllegalArgumentException("Supplier Bill record cannot be null.");
		}

		if (migrationRequest == null) {
			throw new IllegalArgumentException("Migration request cannot be null.");
		}

		String tenantId = migrationRequest.getTenantId();

		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is required for Supplier Bill migration.");
		}

		SupplierBillCreateRequest request = new SupplierBillCreateRequest();
		RequestInfo requestInfo = requestInfoBuilder.build(tenantId);
		request.setRequestInfo(requestInfo);

		request.setTenantId(tenantId);

		SupplierBillRequest billRequest = new SupplierBillRequest();
		billRequest.setWorkFlowAction("Create And Approve");
		billRequest.setApprovalPosition(0L);
		billRequest.setApprovalComment("");
		billRequest.setApprovalDesignation("");

		EgBillregister billRegister = buildBillRegister(record, tenantId, requestInfo);
		billRequest.setEgBillregister(billRegister);

		if (!hasValue(record.getPurchaseOrder())) {
			throw new IllegalArgumentException("Purchase Order number is missing for Excel rows " + record.getStartRow()
					+ "-" + record.getEndRow());
		}

		List<EgBillPurchaseItemsDTO> workItems = purchaseOrderServiceClient
				.getPurchaseItemsByOrderNumber(record.getPurchaseOrder(), requestInfo, tenantId);

		if (workItems == null) {
			workItems = new ArrayList<>();
		}

		billRequest.setPurchaseItemsForBillRegister(workItems);
		request.setSupplierBillRequest(billRequest);

		return request;
	}

	/**
	 * Build EgBillregister.
	 */

	private EgBillregister buildBillRegister(SupplierBillRecord record, String tenantId, RequestInfo requestInfo) {

		EgBillregister bill = new EgBillregister();
		bill.setBillnumber(generateBillNumber(record));

		if (!hasValue(record.getBillDate())) {
			throw new IllegalArgumentException(
					"Bill date is missing for Excel rows " + record.getStartRow() + "-" + record.getEndRow());
		}

		bill.setBilldate(record.getBillDate());
		bill.setBilltype(record.getBillType());
		bill.setWorkordernumber(record.getPurchaseOrder());
		bill.setExpendituretype("Purchase");

		/*
		 * ========================================================= BILL MIS
		 * =========================================================
		 */
		EgBillregistermis mis = buildMIS(record, tenantId, requestInfo);

		bill.setEgBillregistermis(mis);

		/*
		 * ========================================================= DEBIT DETAILS
		 * =========================================================
		 *
		 * Excel gives GL CODE. Finance API requires GL ID.
		 */
		List<EgBillDetails> debitDetails = buildDebitDetails(record.getDebitDetails(), requestInfo, tenantId);
		bill.setDebitDetails(debitDetails);

		/*
		 * ========================================================= BILL AMOUNT /
		 * PASSED AMOUNT =========================================================
		 *
		 * Both amounts are based on the total debit amount.
		 */
		BigDecimal totalDebitAmount = debitDetails.stream().map(EgBillDetails::getDebitamount).filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		if (totalDebitAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Total debit amount must be greater than zero for Excel rows "
					+ record.getStartRow() + "-" + record.getEndRow());
		}

		bill.setBillamount(totalDebitAmount);
		bill.setPassedamount(totalDebitAmount);

		/*
		 * ========================================================= CREDIT DETAILS
		 * =========================================================
		 */
		bill.setCreditDetails(buildCreditDetails(record.getCreditDetails(), requestInfo, tenantId));

		/*
		 * ========================================================= NET PAYABLE DETAILS
		 * =========================================================
		 */
		bill.setNetPayableDetails(buildNetPayableDetails(record.getNetPayableDetails(), requestInfo, tenantId));

		/*
		 * ========================================================= PAYEE DETAILS
		 * =========================================================
		 *
		 * No payee/subledger data is currently read from Excel.
		 */
		// bill.setBillPayeedetails(new ArrayList<EgBillPayeedetails>());

		return bill;
	}

	/**
	 * Build MIS information.
	 */
	private EgBillregistermis buildMIS(SupplierBillRecord record, String tenantId, RequestInfo requestInfo) {

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
				throw new IllegalArgumentException(
						"Department Code not found for department: " + record.getDepartment());
			}
			mis.setDepartmentcode(departmentCode);
		}

		/*
		 * ============================== FUNCTION ============================== Excel:
		 * General Administration Finance: { "id": 1 }
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
		 * ====================== SCHEME ====================== Excel: AMRUT Finance:
		 * schemeId = 2
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

			if (hasValue(schemeResponse.getName())) {
				if (!hasValue(schemeResponse.getName())) {
					throw new IllegalArgumentException(
							"Scheme is required to search Sub Scheme: " + schemeResponse.getName());
				}

				SubScheme subSchemeResponse = subSchemeServiceClient.getSubSchemeByName(record.getSubScheme(),
						schemeResponse.getName(), requestInfo, tenantId);

				if (subSchemeResponse == null || subSchemeResponse.getId() == null) {
					throw new IllegalArgumentException(
							"Sub Scheme :" + record.getSubScheme() + " not found for scheme: " + record.getScheme());
				}

				mis.setSubSchemeId(subSchemeResponse.getId());
			}
		}

		mis.setNarration(record.getNarration());
		mis.setPartyBillNumber(record.getPartyBillNo());
		mis.setPartyBillDate(record.getPartyBillDate());
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
//	private String generateBillNumber(SupplierBillRecord record) {
//		return String.format("SUP-BILL-MIG-%05d", record.getStartRow());
//	}

	/**
	 * Generate migration bill number.
	 *
	 * Example: SUP-BILL/24-25/00005
	 */
	private String generateBillNumber(SupplierBillRecord record) {

		if (record == null) {
			throw new IllegalArgumentException("SupplierBillRecord is null.");
		}

		if (record.getSerialNumber() == null) {
			throw new IllegalArgumentException("Serial Number (SN) is required to generate Bill Number.");
		}

		if (record.getSerialNumber() <= 0) {
			throw new IllegalArgumentException(
					"Serial Number (SN) must be greater than zero. Value: " + record.getSerialNumber());
		}

		if (!hasValue(record.getBillDate())) {
			throw new IllegalArgumentException("Bill Date is required to generate Bill Number.");
		}

		LocalDate billDate = LocalDate.parse(record.getBillDate());
		int startYear = billDate.getMonthValue() >= 4 ? billDate.getYear() : billDate.getYear() - 1;
		String financialYear = String.format("%02d-%02d", startYear % 100, (startYear + 1) % 100);
		return String.format("SUP-BILL/%s/%05d", financialYear, record.getSerialNumber());
	}
	

	

	private boolean hasValue(String value) {
		return value != null && !value.trim().isEmpty();
	}
}