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

import tools.jackson.databind.ObjectMapper;

@Service
public class ExpenseBillRequestBuilder {

	private final ObjectMapper objectMapper;
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
			SchemeServiceClient schemeServiceClient, ObjectMapper objectMapper) {

		this.requestInfoBuilder = requestInfoBuilder;
		this.fundServiceClient = fundServiceClient;
		this.accountDetailTypeServiceClient = accountDetailTypeServiceClient;
		this.accountDetailKeyServiceClient = accountDetailKeyServiceClient;
		this.chartOfAccountsServiceClient = chartOfAccountsServiceClient;
		this.functionServiceClient = functionServiceClient;
		this.schemeServiceClient = schemeServiceClient;
		this.objectMapper = objectMapper;
	}

	/**
	 * One ExpenseBillRecord = One Expense Bill API Request.
	 */
	public ExpenseBillCreateRequest build(ExpenseBillRecord record, MigrationRequest migrationRequest) {

		ExpenseBillCreateRequest request = new ExpenseBillCreateRequest();
		String tenantId = migrationRequest.getTenantId();
		request.setTenantId(tenantId);

		RequestInfo requestInfo = requestInfoBuilder.build(tenantId);
		request.setRequestInfo(requestInfo);

		request.setExpenseBillRequest(buildExpenseBillRequest(record, requestInfo, tenantId));

		return request;
	}

	/**
	 * Build:
	 *
	 * "expenseBillRequest": { "workFlowAction": "Create And Approve",
	 * "approvalPosition": 0, "approvalComment": "", "approvalDesignation": "" }
	 */
	private ExpenseBillRequest buildExpenseBillRequest(ExpenseBillRecord record, RequestInfo requestInfo,
			String tenantId) {

		ExpenseBillRequest expenseBillRequest = new ExpenseBillRequest();

		expenseBillRequest.setWorkFlowAction("Create And Approve");
		expenseBillRequest.setApprovalPosition(0L);
		expenseBillRequest.setApprovalComment("");
		expenseBillRequest.setApprovalDesignation("");

		/*
		 * egBillregister
		 */

		try {
			EgBillregister buildBillRegister = buildBillRegister(record, requestInfo, tenantId);
			expenseBillRequest.setEgBillregister(buildBillRegister);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {

			String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(expenseBillRequest);

			System.out.println();
			System.out.println("==========================================================");
			System.out.println("        EXPENSE BILL REQUEST JSON");
			System.out.println("==========================================================");
			System.out.println(requestJson);
			System.out.println("==========================================================");

		} catch (Exception e) {

			System.err.println("ERROR WHILE CONVERTING REQUEST TO JSON");
			e.printStackTrace();
		}

		return expenseBillRequest;
	}

	/**
	 * Build:
	 *
	 * "egBillregister": { "billamount": 30000.00, "billnumber": "EXP-BILL-012",
	 * "billdate": "2026-08-06", "expendituretype": "Expense" }
	 */
	private EgBillregister buildBillRegister(ExpenseBillRecord record, RequestInfo requestInfo, String tenantId)
			throws Exception {

		EgBillregister billRegister = new EgBillregister();

		/*
		 * Calculate total amount
		 */
		BigDecimal billAmount = calculateBillAmount(record);
		billRegister.setBillamount(billAmount);
		billRegister.setBillnumber(generateBillNumber(record));
		String convertToApiDate = convertToApiDate(record.getBillDate());
		billRegister.setBilldate(convertToApiDate);
		billRegister.setExpendituretype("Expense");
		billRegister.setEgBillregistermis(buildMisDetails(record, requestInfo, tenantId));
		billRegister.setBillDetails(buildBillDetails(record, requestInfo, tenantId));
		billRegister.setBillPayeedetails(buildPayeeDetails(record, requestInfo, tenantId));
		billRegister.setCheckLists(buildCheckLists());

		try {

			String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(billRegister);

			System.out.println();
			System.out.println("==========================================================");
			System.out.println("        EXPENSE BILL REGISTER JSON");
			System.out.println("==========================================================");
			System.out.println(requestJson);
			System.out.println("==========================================================");

		} catch (Exception e) {

			System.err.println("ERROR WHILE CONVERTING REQUEST TO JSON");
			e.printStackTrace();
		}
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

		EgBillregistermis mis = new EgBillregistermis();
		requestInfo.setAction("_search");

		/***
		 * call fund fetch api client then set id
		 */

		if (hasValue(record.getFund())) {
			Fund fundResponse = fundServiceClient.getFundByName(record.getFund(), requestInfo, tenantId);
			if (fundResponse == null) {
				throw new IllegalArgumentException("Fund not found: " + record.getFund());
			}

			if (!hasValue(fundResponse.getCode())) {
				throw new IllegalArgumentException("Fund code not found for fund: " + record.getFund());
			}
			mis.setFund(createIdReference(fundResponse.getId()));
		}

		/***
		 * call Scheme fetch api client then set id
		 */

		if (hasValue(record.getScheme())) {
			Scheme schemeByName = schemeServiceClient.getSchemeByName(record.getScheme(), record.getFund(), requestInfo,
					tenantId);
			if (schemeByName == null) {
				throw new IllegalArgumentException("Scheme not found: " + record.getScheme());
			}

			if (!hasValue(schemeByName.getCode())) {
				throw new IllegalArgumentException("Fund code not found for fund: " + record.getFund());
			}
			mis.setSchemeId(schemeByName.getId());
		}

		/***
		 * call Sub-Scheme fetch api client then set id
		 */
//		mis.setSubSchemeId(record.getSubScheme());

		/***
		 * call Function fetch api client then set id
		 */

		if (hasValue(record.getFunction())) {
			Function functionByName = functionServiceClient.getFunctionByName(record.getFunction(), requestInfo,
					tenantId);
			if (functionByName == null) {
				throw new IllegalArgumentException("Function not found: " + record.getScheme());
			}

			mis.setFunction(new IdDTO(functionByName.getId()));
		}

		mis.setFundsource(null);
		String departmentName = record.getDepartment();
		String departmentCode = DepartmentMapping.getDepartmentCode(departmentName);
		mis.setDepartmentcode(departmentCode);
		mis.setNarration(defaultString(record.getNarration()));
		mis.setPartyBillNumber(defaultString(record.getPartyBillNo()));
		String convertToApiDate = convertToApiDate(record.getPartyBillDate());
		mis.setPartyBillDate(convertToApiDate);

		/*
		 * Bill Sub Type
		 */
		if (record.getBillSubType() != null) {

			if (hasValue(record.getBillSubType())) {
				Long billSubTypeId = BillSubtypeMapping.getBillSubTypeId(record.getBillSubType());

				if (billSubTypeId == null) {
					throw new IllegalArgumentException("Invalid Bill Sub Type: " + record.getBillSubType());
				}
				mis.setEgBillSubType(new IdDTO(billSubTypeId));
			}
		}
		mis.setPayto(record.getSubLedgerMaster());

//		System.out.println("====================================");
//		System.out.println("MIS Detail Build");
//		System.out.println("Fund : " + mis.getFund().getId());
//		System.out.println("Scheme : " + mis.getSchemeId());
//		System.out.println("Function Name : " + mis.getFunction());
//		System.out.println("Department : " + mis.getDepartmentcode());
//		System.out.println("Narration : " + mis.getNarration());
//		System.out.println("Part Bill Date : " + mis.getPartyBillDate());
//		System.out.println("Part Bill Number : " + mis.getPartyBillNumber());
//		System.out.println("Bill Sub Type : " + mis.getEgBillSubType());
//		System.out.println("Sub Ledger Master(Payto) : " + mis.getPayto());
//		System.out.println("====================================");

		try {

			String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mis);

			System.out.println();
			System.out.println("==========================================================");
			System.out.println("        EXPENSE BILL REGISTER JSON");
			System.out.println("==========================================================");
			System.out.println(requestJson);
			System.out.println("==========================================================");

		} catch (Exception e) {

			System.err.println("ERROR WHILE CONVERTING REQUEST TO JSON");
			e.printStackTrace();
		}
		return mis;
	}

	/**
	 * Build:
	 *
	 * "billDetails": [ { "glcodeid": 786, "debitamount": 30000.00 }, { "glcodeid":
	 * 1015, "creditamount": 1500.00 } ]
	 */

	private List<EgBilldetails> buildBillDetails(ExpenseBillRecord record, RequestInfo requestInfo, String tenantId) {

		List<EgBilldetails> billDetails = new ArrayList<>();

		requestInfo.setAction("_search");

		if (record.getDebitDetails() == null && record.getDeductionDetails() == null) {

			return billDetails;
		}

		BigDecimal totalDebit = BigDecimal.ZERO;
		BigDecimal totalCredit = BigDecimal.ZERO;

		// ============================================
		// DEBIT DETAILS
		// ============================================
		if (record.getDebitDetails() != null) {

			for (ExpenseDebitRecord sourceDetail : record.getDebitDetails()) {

				EgBilldetails billDetail = new EgBilldetails();

				String numericGlCode = extractNumericGlCode(sourceDetail.getGlCode());

				ChartOfAccountsResponse chartOfAccounts = chartOfAccountsServiceClient.getByGlCode(numericGlCode,
						requestInfo, tenantId);

				billDetail.setGlcodeid(chartOfAccounts.getId());

				billDetail.setDebitamount(sourceDetail.getDebitAmount());

				totalDebit = totalDebit
						.add(sourceDetail.getDebitAmount() != null ? sourceDetail.getDebitAmount() : BigDecimal.ZERO);

				billDetails.add(billDetail);
			}
		}

		// ============================================
		// CREDIT / DEDUCTION DETAILS
		// ============================================
		if (record.getDeductionDetails() != null) {

			for (ExpenseDeductionRecord sourceDetail : record.getDeductionDetails()) {

				EgBilldetails billDetail = new EgBilldetails();

				String numericGlCode = extractNumericGlCode(sourceDetail.getGlCode());

				ChartOfAccountsResponse chartOfAccounts = chartOfAccountsServiceClient.getByGlCode(numericGlCode,
						requestInfo, tenantId);

				billDetail.setGlcodeid(chartOfAccounts.getId());

				billDetail.setCreditamount(sourceDetail.getCreditAmount());

				totalCredit = totalCredit
						.add(sourceDetail.getCreditAmount() != null ? sourceDetail.getCreditAmount() : BigDecimal.ZERO);

				billDetails.add(billDetail);
			}
		}

		// ============================================
		// NET PAYABLE
		// Net Payable = Total Debit - Total Deduction
		// ============================================
		BigDecimal netPayableAmount = totalDebit.subtract(totalCredit);

		if (netPayableAmount.compareTo(BigDecimal.ZERO) > 0 && record.getNetPayableDetail() != null) {
			String numericNetPayableGlCode = extractNumericGlCode(record.getNetPayableDetail().getGlCode());
			ChartOfAccountsResponse netPayableGl = chartOfAccountsServiceClient.getByGlCode(numericNetPayableGlCode,
					requestInfo, tenantId);

			EgBilldetails netPayableDetail = new EgBilldetails();

			netPayableDetail.setGlcodeid(netPayableGl.getId());

			netPayableDetail.setCreditamount(netPayableAmount);

			billDetails.add(netPayableDetail);
		}

		// ============================================
		// DEBUG
		// ============================================
		System.out.println("======================================");
		System.out.println("TOTAL DEBIT       : " + totalDebit);
		System.out.println("TOTAL DEDUCTION   : " + totalCredit);
		System.out.println("NET PAYABLE       : " + netPayableAmount);
		System.out.println("======================================");

		return billDetails;
	}

	private String extractNumericGlCode(String glCodeValue) {

		if (glCodeValue != null && glCodeValue.contains("-")) {
			return glCodeValue.substring(0, glCodeValue.indexOf("-")).trim();
		}

		return glCodeValue != null ? glCodeValue.trim() : null;
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

		List<EgBillPayeedetails> payeeDetails = new ArrayList<EgBillPayeedetails>();
		requestInfo.setAction("_search");

		if (record.getNetPayableDetail() == null) {
			return payeeDetails;
		}

		ExpenseNetPayableRecord sourcePayee = record.getNetPayableDetail();
		/***
		 * call glocde api client to fetch data and set the id
		 */
		String numericGlCode = extractNumericGlCode(sourcePayee.getGlCode());
		ChartOfAccountsResponse chartOfAccounts = chartOfAccountsServiceClient.getByGlCode(numericGlCode, requestInfo,
				tenantId);
		Accountdetailtype accountdetailtype = accountDetailTypeServiceClient.getByName(record.getSubLedgerType(),
				requestInfo, tenantId);

		EgBillPayeedetails billPayeedetails = new EgBillPayeedetails();
		EgBillDetailsIdDTO egBillDetailsIdDTO = new EgBillDetailsIdDTO();
		egBillDetailsIdDTO.setGlcodeid(chartOfAccounts.getId().longValue());
		billPayeedetails.setEgBilldetailsId(egBillDetailsIdDTO);
		billPayeedetails.setIsDebit(false);
		billPayeedetails.setCreditAmount(sourcePayee.getCreditAmount());
		billPayeedetails.setAccountDetailTypeId(accountdetailtype.getId().longValue());
		Accountdetailkey accountDetailKey = accountDetailKeyServiceClient.getAccountDetailKey(accountdetailtype.getId(),
				record.getSubLedgerMaster(), requestInfo, tenantId);
		billPayeedetails.setAccountDetailKeyId(accountDetailKey.getDetailkey().longValue());
		payeeDetails.add(billPayeedetails);

		try {

			String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payeeDetails);

			System.out.println();
			System.out.println("==========================================================");
			System.out.println("        EXPENSE BILL REGISTER JSON");
			System.out.println("==========================================================");
			System.out.println(requestJson);
			System.out.println("==========================================================");

		} catch (Exception e) {

			System.err.println("ERROR WHILE CONVERTING REQUEST TO JSON");
			e.printStackTrace();
		}

		System.out.println("====================================================");
		return payeeDetails;
	}

	/**
	 * Build:
	 *
	 * "checkLists": [ { "appconfigvalue": { "id": 67 }, "checklistvalue": "na" } ]
	 */

	private List<EgBillChecklist> buildCheckLists() {

		List<EgBillChecklist> checkLists = new ArrayList<EgBillChecklist>();

		checkLists.add(createCheckList(67L));
		checkLists.add(createCheckList(68L));
		checkLists.add(createCheckList(69L));
		checkLists.add(createCheckList(70L));

		return checkLists;
	}

	/**
	 * Create one checklist item.
	 */
	private EgBillChecklist createCheckList(Long id) {
		EgBillChecklist checkList = new EgBillChecklist();
		checkList.setAppconfigvalue(new IdDTO(id));
		checkList.setChecklistvalue("na");
		return checkList;
	}

	/**
	 * Create:
	 *
	 * { "id": value }
	 */

	private String convertToApiDate(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
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
			}
		}
		throw new IllegalArgumentException("Invalid date: " + value + ". Expected dd/MM/yyyy or yyyy-MM-dd");
	}

	/**
	 * Calculate total bill amount.
	 *
	 * Total Debit Amount + Total Credit Amount.
	 */
	private BigDecimal calculateBillAmount(ExpenseBillRecord record) {

		BigDecimal totalAmount = BigDecimal.ZERO;

		/*
		 * Add all Net  Payable amount amounts
		 */
		if (record.getNetPayableDetail() != null) {
			ExpenseNetPayableRecord netPayableRecord = record.getNetPayableDetail();
			if (netPayableRecord.getCreditAmount() != null) {
				totalAmount = totalAmount.add(netPayableRecord.getCreditAmount());
			}
		}

		/*
		 * Add all Deduction amount (credit) amounts
		 */
		if (record.getDeductionDetails() != null) {
			for (ExpenseDeductionRecord creditDetail : record.getDeductionDetails()) {
				if (creditDetail.getCreditAmount() != null) {
					totalAmount = totalAmount.add(creditDetail.getCreditAmount());
				}
			}
		}
		return totalAmount;
	}

	private String generateBillNumber(ExpenseBillRecord record) {

		if (record.getSerialNumber() == null) {
			throw new IllegalArgumentException("Serial number is required to generate bill number");
		}

		return String.format("EXP-BILL-%05d", record.getSerialNumber());
	}

	private IdDTO createIdReference(Long id) {
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