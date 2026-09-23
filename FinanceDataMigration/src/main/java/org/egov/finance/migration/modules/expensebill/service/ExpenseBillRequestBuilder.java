package org.egov.finance.migration.modules.expensebill.service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.egov.finance.migration.common.dto.ChartOfAccountsResponse;
import org.egov.finance.migration.common.dto.Function;
import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.dto.Scheme;
import org.egov.finance.migration.common.util.AccountDetailKeyServiceClient;
import org.egov.finance.migration.common.util.AccountDetailTypeServiceClient;
import org.egov.finance.migration.common.util.Accountdetailkey;
import org.egov.finance.migration.common.util.Accountdetailtype;
import org.egov.finance.migration.common.util.ChartOfAccountsServiceClient;
import org.egov.finance.migration.common.util.DepartmentMapping;
import org.egov.finance.migration.common.util.FunctionServiceClient;
import org.egov.finance.migration.common.util.FundServiceClient;
import org.egov.finance.migration.common.util.SchemeServiceClient;
import org.egov.finance.migration.modules.expensebill.dto.EgBillChecklist;
import org.egov.finance.migration.modules.expensebill.dto.EgBillDetailsIdDTO;
import org.egov.finance.migration.modules.expensebill.dto.EgBillPayeedetails;
import org.egov.finance.migration.modules.expensebill.dto.EgBilldetails;
import org.egov.finance.migration.modules.expensebill.dto.EgBillregister;
import org.egov.finance.migration.modules.expensebill.dto.EgBillregistermis;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillCreateRequest;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRecord;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRequest;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseDebitRecord;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseDeductionRecord;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseNetPayableRecord;
import org.egov.finance.migration.modules.expensebill.dto.IdDTO;
import org.springframework.stereotype.Service;

@Service
public class ExpenseBillRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;
	private final FundServiceClient fundServiceClient;
	private final FunctionServiceClient functionServiceClient;
	private final SchemeServiceClient schemeServiceClient;
	private final AccountDetailTypeServiceClient accountDetailTypeServiceClient;
	private final AccountDetailKeyServiceClient accountDetailKeyServiceClient;
	private final ChartOfAccountsServiceClient chartOfAccountsServiceClient;

	public ExpenseBillRequestBuilder(RequestInfoBuilder requestInfoBuilder, FundServiceClient fundServiceClient,
			AccountDetailTypeServiceClient accountDetailTypeServiceClient,
			AccountDetailKeyServiceClient accountDetailKeyServiceClient,
			ChartOfAccountsServiceClient chartOfAccountsServiceClient, FunctionServiceClient functionServiceClient,
			SchemeServiceClient schemeServiceClient) {

		this.requestInfoBuilder = requestInfoBuilder;
		this.fundServiceClient = fundServiceClient;
		this.accountDetailTypeServiceClient = accountDetailTypeServiceClient;
		this.accountDetailKeyServiceClient = accountDetailKeyServiceClient;
		this.chartOfAccountsServiceClient = chartOfAccountsServiceClient;
		this.functionServiceClient = functionServiceClient;
		this.schemeServiceClient = schemeServiceClient;
	}

	/**
	 * One ExpenseBillRecord = One Expense Bill API Request.
	 */

	public ExpenseBillCreateRequest build(ExpenseBillRecord record, MigrationRequest migrationRequest) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null.");
		}

		if (record.getSerialNumber() == null) {
			throw new IllegalArgumentException("Serial Number (SN) is missing.");
		}

		if (migrationRequest == null) {
			throw new IllegalArgumentException("MigrationRequest is null for SN: " + record.getSerialNumber());
		}

		String tenantId = migrationRequest.getTenantId();

		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is missing for SN: " + record.getSerialNumber());
		}

		try {

			ExpenseBillCreateRequest request = new ExpenseBillCreateRequest();
			request.setTenantId(tenantId);
			RequestInfo requestInfo = requestInfoBuilder.build(tenantId);

			if (requestInfo == null) {
				throw new IllegalArgumentException("RequestInfo could not be created for tenant: " + tenantId);
			}

			request.setRequestInfo(requestInfo);
			ExpenseBillRequest expenseBillRequest = buildExpenseBillRequest(record, requestInfo, tenantId);

			if (expenseBillRequest == null) {
				throw new IllegalArgumentException("ExpenseBillRequest could not be created.");
			}

			if (!hasValue(expenseBillRequest.getWorkFlowAction())) {
				throw new IllegalArgumentException("Workflow Action is missing.");
			}

			if (expenseBillRequest.getApprovalPosition() == null) {
				throw new IllegalArgumentException("Approval Position is missing.");
			}

			/*
			 * ===================================================== 7. Validate
			 * EgBillregister =====================================================
			 */
			EgBillregister billRegister = expenseBillRequest.getEgBillregister();

			if (billRegister == null) {
				throw new IllegalArgumentException("EgBillregister could not be created.");
			}
			if (billRegister.getBillamount() == null) {
				throw new IllegalArgumentException("Bill Amount is missing.");
			}
			if (billRegister.getBillamount().compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException(
						"Bill Amount must be greater than zero. " + "Value: " + billRegister.getBillamount());
			}
			if (!hasValue(billRegister.getBillnumber())) {
				throw new IllegalArgumentException("Bill Number is missing.");
			}
			if (!hasValue(billRegister.getBilldate())) {
				throw new IllegalArgumentException("Bill Date is missing.");
			}
			if (!hasValue(billRegister.getExpendituretype())) {
				throw new IllegalArgumentException("Expenditure Type is missing.");
			}
			if (billRegister.getEgBillregistermis() == null) {
				throw new IllegalArgumentException("EgBillregister MIS details are missing.");
			}
			if (billRegister.getBillDetails() == null || billRegister.getBillDetails().isEmpty()) {
				throw new IllegalArgumentException("Bill Details are missing.");
			}
			if (billRegister.getBillPayeedetails() == null || billRegister.getBillPayeedetails().isEmpty()) {
				throw new IllegalArgumentException("Bill Payee Details are missing.");
			}
			if (billRegister.getCheckLists() == null || billRegister.getCheckLists().isEmpty()) {
				throw new IllegalArgumentException("Bill Checklist details are missing.");
			}

			request.setExpenseBillRequest(expenseBillRequest);
			return request;

		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(
					"Expense Bill build failed for SN " + record.getSerialNumber() + ": " + e.getMessage(), e);

		} catch (Exception e) {
			throw new IllegalArgumentException("Unexpected error while building Expense Bill " + "for SN "
					+ record.getSerialNumber() + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Build:
	 *
	 * "expenseBillRequest": { "workFlowAction": "Create And Approve",
	 * "approvalPosition": 0, "approvalComment": "", "approvalDesignation": "" }
	 * 
	 * @throws Exception
	 */

	private ExpenseBillRequest buildExpenseBillRequest(ExpenseBillRecord record, RequestInfo requestInfo,
			String tenantId) throws Exception {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null.");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is null.");
		}

		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is missing.");
		}

		ExpenseBillRequest expenseBillRequest = new ExpenseBillRequest();

		expenseBillRequest.setWorkFlowAction("Create And Approve");
		expenseBillRequest.setApprovalPosition(0L);
		expenseBillRequest.setApprovalComment("");
		expenseBillRequest.setApprovalDesignation("");

		/*
		 * Build Bill Register
		 */
		EgBillregister billRegister = buildBillRegister(record, requestInfo, tenantId);

		if (billRegister == null) {
			throw new IllegalArgumentException("EgBillregister could not be created.");
		}

		expenseBillRequest.setEgBillregister(billRegister);

		return expenseBillRequest;
	}

	/**
	 * Build:
	 *
	 * "egBillregister": { "billamount": 30000.00, "billnumber": "EXP-BILL-012",
	 * "billdate": "2026-08-06", "expendituretype": "Expense" }
	 */


	private EgBillregister buildBillRegister(ExpenseBillRecord record, RequestInfo requestInfo, String tenantId) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null.");
		}
		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is null.");
		}
		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is missing.");
		}

		EgBillregister billRegister = new EgBillregister();
		BigDecimal billAmount = calculateBillAmount(record);

		if (billAmount == null) {
			throw new IllegalArgumentException("Bill Amount could not be calculated.");
		}

		if (billAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException(
					"Calculated Bill Amount must be greater than zero. " + "Value: " + billAmount);
		}

		billRegister.setBillamount(billAmount);
		String billNumber = generateBillNumber(record);

		if (!hasValue(billNumber)) {
			throw new IllegalArgumentException("Bill Number could not be generated.");
		}

		billRegister.setBillnumber(billNumber);
		if (!hasValue(record.getBillDate())) {
			throw new IllegalArgumentException("Bill Date is missing for SN: " + record.getSerialNumber());
		}

		String billDate = convertToApiDate(record.getBillDate(), "Bill Date");
		billRegister.setBilldate(billDate);
		billRegister.setExpendituretype("Expense");

		if (!hasValue(billRegister.getExpendituretype())) {
			throw new IllegalArgumentException("Expenditure Type is missing for bill: " + billNumber);
		}

		/*
		 * MIS
		 */
		EgBillregistermis misDetails = buildMisDetails(record, requestInfo, tenantId);

		if (misDetails == null) {
			throw new IllegalArgumentException("MIS details could not be created for bill: " + billNumber);
		}

		billRegister.setEgBillregistermis(misDetails);

		/*
		 * Bill Details
		 */
		List<EgBilldetails> billDetails = buildBillDetails(record, requestInfo, tenantId);

		if (billDetails == null || billDetails.isEmpty()) {
			throw new IllegalArgumentException("Bill Details could not be created for bill: " + billNumber);
		}

		billRegister.setBillDetails(billDetails);

		/*
		 * Payee Details
		 */
		List<EgBillPayeedetails> payeeDetails = buildPayeeDetails(record, requestInfo, tenantId);

		if (payeeDetails == null || payeeDetails.isEmpty()) {
			throw new IllegalArgumentException("Payee Details could not be created for bill: " + billNumber);
		}

		billRegister.setBillPayeedetails(payeeDetails);

		/*
		 * Checklist
		 */
		List<EgBillChecklist> checkLists = buildCheckLists();

		if (checkLists == null || checkLists.isEmpty()) {
			throw new IllegalArgumentException("Checklist details could not be created for bill: " + billNumber);
		}

		billRegister.setCheckLists(checkLists);
		return billRegister;
	}

	/**
	 * Build:
	 *
	 * "egBillregistermis": { "fund": {"id": 1}, "schemeId": 2, "subSchemeId": 2,
	 * "function": {"id": 1}, "fundsource": null, "departmentcode": "DEPT_1",
	 * "narration": "", "partyBillNumber": "", "partyBillDate": null,
	 * "egBillSubType": {"id": 11}, "payto": "Raju kumar" }
	 */

	private EgBillregistermis buildMisDetails(ExpenseBillRecord record, RequestInfo requestInfo, String tenantId) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null while building MIS details.");
		}
		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is null while building MIS details.");
		}
		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is missing while building MIS details.");
		}

		EgBillregistermis mis = new EgBillregistermis();
		requestInfo.setAction("_search");

		/*
		 * FUND
		 */
		if (!hasValue(record.getFund())) {
			throw new IllegalArgumentException("Fund is missing for SN: " + record.getSerialNumber());
		}

		Fund fundResponse = fundServiceClient.getFundByName(record.getFund(), requestInfo, tenantId);

		if (fundResponse == null) {
			throw new IllegalArgumentException(
					"Fund not found: " + record.getFund() + " Please Create or Check active/Inactive.");
		}
		if (fundResponse.getId() == null) {
			throw new IllegalArgumentException("Fund ID is missing from API response for fund: " + record.getFund());
		}
		if (!hasValue(fundResponse.getCode())) {
			throw new IllegalArgumentException("Fund code is missing from API response for fund: " + record.getFund());
		}

		mis.setFund(createIdReference(fundResponse.getId()));

		/*
		 * SCHEME
		 */
		if (hasValue(record.getScheme())) {
			
			Scheme scheme = schemeServiceClient.getSchemeByName(record.getScheme(), record.getFund(), requestInfo,
					tenantId);

			if (scheme == null) {
				throw new IllegalArgumentException("Scheme not found: " + record.getScheme() + " for Fund: " + record.getFund());
			}
			if (scheme.getId() == null) {
				throw new IllegalArgumentException("Scheme ID is missing from API response for scheme: " + record.getScheme());
			}
			if (!hasValue(scheme.getCode())) {
				throw new IllegalArgumentException("Scheme code is missing from API response for scheme: " + record.getScheme());
			}
			mis.setSchemeId(scheme.getId());
		}
		

		/*
		 * FUNCTION
		 */
		if (!hasValue(record.getFunction())) {
			throw new IllegalArgumentException("Function is missing for SN: " + record.getSerialNumber());
		}

		Function function = functionServiceClient.getFunctionByName(record.getFunction(), requestInfo, tenantId);

		if (function == null) {
			throw new IllegalArgumentException("Function not found: " + record.getFunction());
		}

		if (function.getId() == null) {
			throw new IllegalArgumentException("Function ID is missing from API response for function: " + record.getFunction());
		}

		mis.setFunction(new IdDTO(function.getId()));

		/*
		 * DEPARTMENT
		 */
		if (!hasValue(record.getDepartment())) {
			throw new IllegalArgumentException("Department is missing for SN: " + record.getSerialNumber());
		}

		String departmentCode = DepartmentMapping.getDepartmentCode(record.getDepartment());

		if (!hasValue(departmentCode)) {
			throw new IllegalArgumentException(
					"Department mapping not found for department: " + record.getDepartment());
		}

		mis.setDepartmentcode(departmentCode);

		/*
		 * FUND SOURCE
		 *
		 * Currently your API payload intentionally sets it to null.
		 */
		mis.setFundsource(null);

		/*
		 * NARRATION
		 */
		mis.setNarration(defaultString(record.getNarration()));

		/*
		 * PARTY BILL NUMBER
		 */
		mis.setPartyBillNumber(defaultString(record.getPartyBillNo()));

		/*
		 * PARTY BILL DATE
		 */
		if (hasValue(record.getPartyBillDate())) {
			mis.setPartyBillDate(convertToApiDate(record.getPartyBillDate(), "Party Bill Date"));
		} else {
			mis.setPartyBillDate(null);
		}

		/*
		 * BILL SUB TYPE
		 */
		if (!hasValue(record.getBillSubType())) {
			throw new IllegalArgumentException("Bill Sub Type is missing for SN: " + record.getSerialNumber());
		}

		String billSubType = record.getBillSubType().trim();

		Integer billSubTypeId = BillSubtypeMapping.getBillSubTypeId(billSubType);

		if (billSubTypeId == null) {
			throw new IllegalArgumentException(
					"Invalid Bill Sub Type: " + billSubType + ". Valid values are: Contingent, Salary, Pension, "
							+ "Works, Supplies, Recovery, Deposit, Advance, GPF, " + "Others, Expense");
		}

		mis.setEgBillSubType(new IdDTO(billSubTypeId.longValue()));

		/*
		 * SUB LEDGER MASTER / PAY TO
		 */
		if (!hasValue(record.getSubLedgerMaster())) {
			throw new IllegalArgumentException("Sub Ledger Master is missing for SN: " + record.getSerialNumber());
		}

		mis.setPayto(record.getSubLedgerMaster());

		return mis;
	}

	/**
	 * Build:
	 *
	 * "billDetails": [ { "glcodeid": 786, "debitamount": 30000.00 }, { "glcodeid":
	 * 1015, "creditamount": 1500.00 } ]
	 */

	private ChartOfAccountsResponse getChartOfAccount(String glCode, String fieldName, RequestInfo requestInfo,
			String tenantId) {

		if (!hasValue(glCode)) {
			throw new IllegalArgumentException(fieldName + " GL Code is missing.");
		}

		String numericGlCode = extractNumericGlCode(glCode);

		if (!hasValue(numericGlCode)) {
			throw new IllegalArgumentException("Invalid " + fieldName + " GL Code: " + glCode);
		}

		ChartOfAccountsResponse response = chartOfAccountsServiceClient.getByGlCode(numericGlCode, requestInfo,
				tenantId);

		if (response == null) {
			throw new IllegalArgumentException(
					"Chart of Account not found for " + fieldName + " GL Code: " + numericGlCode);
		}

		if (response.getId() == null) {
			throw new IllegalArgumentException(
					"Chart of Account ID is missing for " + fieldName + " GL Code: " + numericGlCode);
		}

		return response;
	}

	private List<EgBilldetails> buildBillDetails(ExpenseBillRecord record, RequestInfo requestInfo, String tenantId) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null while building Bill Details.");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is null while building Bill Details.");
		}

		List<EgBilldetails> billDetails = new ArrayList<>();
		requestInfo.setAction("_search");

		/*
		 * Debit Details
		 */
		if (record.getDebitDetails() == null || record.getDebitDetails().isEmpty()) {
			throw new IllegalArgumentException("Debit Details are missing.");
		}

		/*
		 * Deduction Details can be empty, but the list itself should not be null.
		 */
		if (record.getDeductionDetails() == null) {
			throw new IllegalArgumentException("Deduction Details list is null.");
		}

		BigDecimal totalDebit = BigDecimal.ZERO;
		BigDecimal totalCredit = BigDecimal.ZERO;

		/*
		 * DEBIT
		 */
		for (int i = 0; i < record.getDebitDetails().size(); i++) {

			ExpenseDebitRecord sourceDetail = record.getDebitDetails().get(i);

			if (sourceDetail == null) {
				throw new IllegalArgumentException("Debit Detail at index " + i + " is null.");
			}
			if (!hasValue(sourceDetail.getGlCode())) {
				throw new IllegalArgumentException("Debit GL Code is missing at index " + i + ".");
			}
			if (sourceDetail.getDebitAmount() == null) {
				throw new IllegalArgumentException("Debit Amount is missing for GL Code: " + sourceDetail.getGlCode());
			}
			if (sourceDetail.getDebitAmount().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Debit Amount cannot be negative for GL Code: "
						+ sourceDetail.getGlCode() + ". Value: " + sourceDetail.getDebitAmount());
			}

			ChartOfAccountsResponse coa = getChartOfAccount(sourceDetail.getGlCode(), "Debit", requestInfo, tenantId);
			EgBilldetails detail = new EgBilldetails();
			detail.setGlcodeid(coa.getId());
			detail.setDebitamount(sourceDetail.getDebitAmount());
			totalDebit = totalDebit.add(sourceDetail.getDebitAmount());
			billDetails.add(detail);
		}

		/*
		 * DEDUCTIONS
		 */
		for (int i = 0; i < record.getDeductionDetails().size(); i++) {

			ExpenseDeductionRecord sourceDetail = record.getDeductionDetails().get(i);

			if (sourceDetail == null) {
				throw new IllegalArgumentException("Deduction Detail at index " + i + " is null.");
			}
			if (!hasValue(sourceDetail.getGlCode())) {
				throw new IllegalArgumentException("Deduction GL Code is missing at index " + i + ".");
			}
			if (sourceDetail.getCreditAmount() == null) {
				throw new IllegalArgumentException(
						"Deduction Credit Amount is missing for GL Code: " + sourceDetail.getGlCode());
			}
			if (sourceDetail.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException(
						"Deduction Credit Amount cannot be negative for GL Code: " + sourceDetail.getGlCode());
			}

			ChartOfAccountsResponse coa = getChartOfAccount(sourceDetail.getGlCode(), "Deduction", requestInfo,
					tenantId);
			EgBilldetails detail = new EgBilldetails();
			detail.setGlcodeid(coa.getId());
			detail.setCreditamount(sourceDetail.getCreditAmount());
			totalCredit = totalCredit.add(sourceDetail.getCreditAmount());
			billDetails.add(detail);
		}

		/*
		 * NET PAYABLE
		 */
		if (record.getNetPayableDetail() == null) {
			throw new IllegalArgumentException("Net Payable Details are missing.");
		}

		ExpenseNetPayableRecord netPayable = record.getNetPayableDetail();

		if (!hasValue(netPayable.getGlCode())) {
			throw new IllegalArgumentException("Net Payable GL Code is missing.");
		}
		if (netPayable.getCreditAmount() == null) {
			throw new IllegalArgumentException("Net Payable Credit Amount is missing.");
		}
		if (netPayable.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException(
					"Net Payable Credit Amount cannot be negative. " + "Value: " + netPayable.getCreditAmount());
		}

		/*
		 * Net Payable = Debit - Deduction
		 */
		BigDecimal calculatedNetPayable = totalDebit.subtract(totalCredit);

		if (calculatedNetPayable.compareTo(BigDecimal.ZERO) <= 0) {

			throw new IllegalArgumentException("Calculated Net Payable must be greater than zero. " + "Total Debit: "
					+ totalDebit + ", Total Deduction: " + totalCredit + ", Calculated Net Payable: "
					+ calculatedNetPayable);
		}

		/*
		 * Compare Excel supplied Net Payable with calculated Net Payable.
		 */
		if (netPayable.getCreditAmount().compareTo(calculatedNetPayable) != 0) {
			throw new IllegalArgumentException("Net Payable amount mismatch. " + "Expected: " + calculatedNetPayable
					+ ", Input: " + netPayable.getCreditAmount());
		}

		ChartOfAccountsResponse netPayableCoa = getChartOfAccount(netPayable.getGlCode(), "Net Payable", requestInfo,
				tenantId);
		EgBilldetails netPayableDetail = new EgBilldetails();
		netPayableDetail.setGlcodeid(netPayableCoa.getId());
		netPayableDetail.setCreditamount(calculatedNetPayable);
		billDetails.add(netPayableDetail);
		return billDetails;
	}

	/**
	 * Build:
	 *
	 * "billPayeedetails": [ { "egBilldetailsId": { "glcodeid": 984 },
	 * "creditAmount": 27000.00, "isDebit": false, "accountDetailTypeId": 12,
	 * "accountDetailKeyId": 2 } ]
	 */


	private List<EgBillPayeedetails> buildPayeeDetails(ExpenseBillRecord record, RequestInfo requestInfo,
			String tenantId) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null while building Payee Details.");
		}
		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is null while building Payee Details.");
		}
		if (record.getNetPayableDetail() == null) {
			throw new IllegalArgumentException("Net Payable Details are missing.");
		}
		if (!hasValue(record.getSubLedgerType())) {
			throw new IllegalArgumentException("Sub Ledger Type is missing.");
		}
		if (!hasValue(record.getSubLedgerMaster())) {
			throw new IllegalArgumentException("Sub Ledger Master is missing.");
		}
		ExpenseNetPayableRecord sourcePayee = record.getNetPayableDetail();

		if (!hasValue(sourcePayee.getGlCode())) {
			throw new IllegalArgumentException("Net Payable GL Code is missing.");
		}
		if (sourcePayee.getCreditAmount() == null) {
			throw new IllegalArgumentException("Net Payable Credit Amount is missing.");
		}
		if (sourcePayee.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Net Payable Credit Amount must be greater than zero.");
		}

		requestInfo.setAction("_search");

		/*
		 * GL
		 */
		ChartOfAccountsResponse coa = getChartOfAccount(sourcePayee.getGlCode(), "Net Payable", requestInfo, tenantId);

		/*
		 * Account Detail Type
		 */
		Accountdetailtype accountDetailType = accountDetailTypeServiceClient.getByName(record.getSubLedgerType(),
				requestInfo, tenantId);

		if (accountDetailType == null) {
			throw new IllegalArgumentException("Account Detail Type not found: " + record.getSubLedgerType());
		}

		if (accountDetailType.getId() == null) {
			throw new IllegalArgumentException("Account Detail Type ID is missing for: " + record.getSubLedgerType());
		}

		/*
		 * Account Detail Key
		 */
		Accountdetailkey accountDetailKey = accountDetailKeyServiceClient.getAccountDetailKey(accountDetailType,
				record.getSubLedgerMaster(), requestInfo, tenantId);

		if (accountDetailKey == null) {
			throw new IllegalArgumentException(
					"Account Detail Key not found for Sub Ledger Master: " + record.getSubLedgerMaster());
		}

		if (accountDetailKey.getDetailkey() == null) {
			throw new IllegalArgumentException(
					"Account Detail Key ID is missing for Sub Ledger Master: " + record.getSubLedgerMaster());
		}

		/*
		 * Build Payee
		 */
		EgBillPayeedetails payee = new EgBillPayeedetails();
		EgBillDetailsIdDTO billDetailsId = new EgBillDetailsIdDTO();
		billDetailsId.setGlcodeid(coa.getId().longValue());
		payee.setEgBilldetailsId(billDetailsId);
		payee.setDebitAmount(null);
		payee.setCreditAmount(sourcePayee.getCreditAmount());
		payee.setIsDebit(false);
		payee.setAccountDetailTypeId(accountDetailType.getId().longValue());
		payee.setAccountDetailKeyId(accountDetailKey.getDetailkey().longValue());
		List<EgBillPayeedetails> result = new ArrayList<>();
		result.add(payee);
		return result;
	}

	/**
	 * Build:
	 *
	 * "checkLists": [ { "appconfigvalue": { "id": 67 }, "checklistvalue": "na" } ]
	 */


	private List<EgBillChecklist> buildCheckLists() {

		Long[] checklistIds = { 67L, 68L, 69L, 70L };

		List<EgBillChecklist> checkLists = new ArrayList<>();

		for (Long id : checklistIds) {

			if (id == null) {
				throw new IllegalArgumentException("Checklist configuration ID is null.");
			}

			if (id <= 0) {
				throw new IllegalArgumentException("Invalid checklist configuration ID: " + id);
			}

			EgBillChecklist checkList = createCheckList(id);

			if (checkList == null) {
				throw new IllegalArgumentException("Checklist could not be created for ID: " + id);
			}

			if (checkList.getAppconfigvalue() == null || checkList.getAppconfigvalue().getId() == null) {

				throw new IllegalArgumentException("Checklist AppConfigValue ID is missing for checklist: " + id);
			}

			if (!hasValue(checkList.getChecklistvalue())) {
				throw new IllegalArgumentException("Checklist value is missing for checklist: " + id);
			}

			checkLists.add(checkList);
		}

		if (checkLists.isEmpty()) {
			throw new IllegalArgumentException("No checklist details were generated.");
		}

		return checkLists;
	}

	/**
	 * Create one checklist item.
	 */


	private EgBillChecklist createCheckList(Long id) {

		if (id == null || id <= 0) {
			throw new IllegalArgumentException("Invalid checklist ID: " + id);
		}

		EgBillChecklist checkList = new EgBillChecklist();

		IdDTO appConfigValue = new IdDTO(id);

		if (appConfigValue.getId() == null) {
			throw new IllegalArgumentException("Checklist AppConfigValue ID could not be created for ID: " + id);
		}

		checkList.setAppconfigvalue(appConfigValue);

		checkList.setChecklistvalue("na");

		return checkList;
	}

	/**
	 * Create:
	 *
	 * { "id": value }
	 */


	private String convertToApiDate(String value, String fieldName) {

		if (!hasValue(value)) {
			throw new IllegalArgumentException(fieldName + " is missing.");
		}

		value = value.trim();
		String[] formats = { "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd-MMM-yyyy" };

		for (String format : formats) {
			try {

				SimpleDateFormat input = new SimpleDateFormat(format);
				input.setLenient(false);
				Date date = input.parse(value);
				SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd");
				return output.format(date);
			} catch (ParseException ignored) {
				// Try next format.
			}
		}
		throw new IllegalArgumentException(
				"Invalid " + fieldName + ": '" + value + "'. " + "Expected format: dd/MM/yyyy.");
	}

	/**
	 * Calculate total bill amount.
	 *
	 * Total Debit Amount + Total Credit Amount.
	 */


	private BigDecimal calculateBillAmount(ExpenseBillRecord record) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null while calculating Bill Amount.");
		}

		if (record.getDebitDetails() == null || record.getDebitDetails().isEmpty()) {
			throw new IllegalArgumentException("Debit Details are required to calculate Bill Amount.");
		}

		BigDecimal totalDebit = BigDecimal.ZERO;
		BigDecimal totalDeduction = BigDecimal.ZERO;

		/*
		 * DEBIT
		 */
		for (ExpenseDebitRecord debit : record.getDebitDetails()) {

			if (debit == null) {
				throw new IllegalArgumentException("Debit Detail contains a null record.");
			}
			if (debit.getDebitAmount() == null) {
				throw new IllegalArgumentException("Debit Amount is missing for GL Code: " + debit.getGlCode());
			}
			if (debit.getDebitAmount().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException("Debit Amount cannot be negative for GL Code: " + debit.getGlCode());
			}
			totalDebit = totalDebit.add(debit.getDebitAmount());
		}

		/*
		 * DEDUCTION
		 */
		if (record.getDeductionDetails() == null) {
			throw new IllegalArgumentException("Deduction Details list is null.");
		}

		for (ExpenseDeductionRecord deduction : record.getDeductionDetails()) {

			if (deduction == null) {
				throw new IllegalArgumentException("Deduction Detail contains a null record.");
			}
			if (deduction.getCreditAmount() == null) {
				throw new IllegalArgumentException("Deduction Credit Amount is missing for GL Code: " + deduction.getGlCode());
			}
			if (deduction.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
				throw new IllegalArgumentException(	"Deduction Credit Amount cannot be negative for GL Code: " + deduction.getGlCode());
			}
			totalDeduction = totalDeduction.add(deduction.getCreditAmount());
		}

		if (totalDebit.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Total Debit Amount must be greater than zero.");
		}

		/*
		 * Calculate Net Payable
		 */
		BigDecimal calculatedNetPayable = totalDebit.subtract(totalDeduction);

		if (calculatedNetPayable.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Calculated Net Payable must be greater than zero. " + "Total Debit: "
					+ totalDebit + ", Total Deduction: " + totalDeduction + ", Net Payable: " + calculatedNetPayable);
		}

		/*
		 * Validate Excel Net Payable
		 */
		if (record.getNetPayableDetail() == null) {
			throw new IllegalArgumentException("Net Payable Details are missing.");
		}

		BigDecimal inputNetPayable = record.getNetPayableDetail().getCreditAmount();

		if (inputNetPayable == null) {
			throw new IllegalArgumentException("Net Payable Credit Amount is missing.");
		}
		if (inputNetPayable.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Net Payable Credit Amount must be greater than zero.");
		}
		if (inputNetPayable.compareTo(calculatedNetPayable) != 0) {
			throw new IllegalArgumentException("Net Payable amount mismatch. " + "Expected: " + calculatedNetPayable + ", Input: " + inputNetPayable);
		}

		/*
		 * Bill Amount = Total Debit
		 */
		return totalDebit;
	}



	private String generateBillNumber(ExpenseBillRecord record) {

		if (record == null) {
			throw new IllegalArgumentException("ExpenseBillRecord is null.");
		}

		if (record.getSerialNumber() == null) {
			throw new IllegalArgumentException("Serial Number (SN) is required to generate Bill Number.");
		}

		if (record.getSerialNumber() <= 0) {
			throw new IllegalArgumentException("Serial Number (SN) must be greater than zero. " + "Value: " + record.getSerialNumber());
		}

		return String.format("EXP-BILL-%05d", record.getSerialNumber());
	}


	private String extractNumericGlCode(String glCodeValue) {

		if (!hasValue(glCodeValue)) {
			throw new IllegalArgumentException("GL Code is missing.");
		}

		String value = glCodeValue.trim();
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(value);

		if (!matcher.find()) {
			throw new IllegalArgumentException("Invalid GL Code format: '" + glCodeValue + "'. Expected a numeric GL Code, " + "for example: 3501000003-Expense Payables.");
		}
		return matcher.group(1);
	}

	private IdDTO createIdReference(Long id) {

		if (id == null) {
			throw new IllegalArgumentException("ID is null while creating ID reference.");
		}
		if (id <= 0) {
			throw new IllegalArgumentException("ID must be greater than zero. Value: " + id);
		}
		IdDTO reference = new IdDTO();
		reference.setId(id);
		return reference;
	}

	private boolean hasValue(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private String defaultString(String value) {
		return value != null ? value : "";
	}

}