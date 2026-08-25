package org.egov.finance.migration.modules.expensebill.service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.util.AccountDetailKeyServiceClient;
import org.egov.finance.migration.common.util.AccountDetailTypeServiceClient;
import org.egov.finance.migration.common.util.Accountdetailkey;
import org.egov.finance.migration.common.util.Accountdetailtype;
import org.egov.finance.migration.common.util.DepartmentMapping;
import org.egov.finance.migration.common.util.FundServiceClient;
import org.egov.finance.migration.modules.expensebill.dto.AppConfigValue;
import org.egov.finance.migration.modules.expensebill.dto.EgBillChecklist;
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
	private final AccountDetailTypeServiceClient accountDetailTypeServiceClient;
	private final AccountDetailKeyServiceClient accountDetailKeyServiceClient;

	public ExpenseBillRequestBuilder(RequestInfoBuilder requestInfoBuilder, FundServiceClient fundServiceClient,
			AccountDetailTypeServiceClient accountDetailTypeServiceClient,
			AccountDetailKeyServiceClient accountDetailKeyServiceClient) {

		this.requestInfoBuilder = requestInfoBuilder;
		this.fundServiceClient = fundServiceClient;
		this.accountDetailTypeServiceClient = accountDetailTypeServiceClient;
		this.accountDetailKeyServiceClient = accountDetailKeyServiceClient;
	}

	/**
	 * One ExpenseBillRecord = One Expense Bill API Request.
	 */
	public ExpenseBillCreateRequest build(ExpenseBillRecord record, MigrationRequest migrationRequest) {

		ExpenseBillCreateRequest request = new ExpenseBillCreateRequest();

		/*
		 * Tenant ID
		 */
		String tenantId = migrationRequest.getTenantId();
		request.setTenantId(tenantId);

		/*
		 * RequestInfo
		 */
		RequestInfo requestInfo = requestInfoBuilder.build(tenantId);
		request.setRequestInfo(requestInfo);

		/*
		 * Expense Bill Request
		 */
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
		expenseBillRequest.setEgBillregister(buildBillRegister(record, requestInfo, tenantId));
		return expenseBillRequest;
	}

	/**
	 * Build:
	 *
	 * "egBillregister": { "billamount": 30000.00, "billnumber": "EXP-BILL-012",
	 * "billdate": "2026-08-06", "expendituretype": "Expense" }
	 */
	private EgBillregister buildBillRegister(ExpenseBillRecord record, RequestInfo requestInfo, String tenantId) {

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
		billRegister.setBillDetails(buildBillDetails(record));
		billRegister.setBillPayeedetails(buildPayeeDetails(record, requestInfo, tenantId));
		billRegister.setCheckLists(buildCheckLists());
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

		/*
		 * Scheme
		 */

		/***
		 * call Scheme fetch api client then set id
		 */
		String scheme = record.getScheme();
		//
//		mis.setSchemeId();

		/*
		 * Sub Scheme
		 */
		/***
		 * call Sub-Scheme fetch api client then set id
		 */
//		mis.setSubSchemeId(record.getSubScheme());

		/*
		 * Function
		 */
		if (record.getFunction() != null) {

			/***
			 * call Function fetch api client then set id
			 */

//			mis.setFunction(createIdReference(record.getFunction()));
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

				mis.setEgBillSubType(billSubTypeId);
			}
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
	private List<EgBilldetails> buildBillDetails(ExpenseBillRecord record) {

		List<EgBilldetails> billDetails = new ArrayList<EgBilldetails>();

		if (record.getDebitDetails() == null && record.getDeductionDetails() == null) {
			return billDetails;
		}

		for (ExpenseDebitRecord sourceDetail : record.getDebitDetails()) {
			EgBilldetails billDetail = new EgBilldetails();
			/***
			 * call glcode fecth client then set glcode id
			 */
//			billDetail.setGlcodeid(sourceDetail.getGlcodeid());
			billDetail.setDebitamount(sourceDetail.getDebitAmount());
			billDetails.add(billDetail);
		}

		for (ExpenseDeductionRecord sourceDetail : record.getDeductionDetails()) {
			EgBilldetails billDetail = new EgBilldetails();
			/***
			 * call glcode fetch client then set glcode id
			 */
//			billDetail.setGlcodeid(sourceDetail.getGlcodeid());
			billDetail.setCreditamount(sourceDetail.getCreditAmount());
			billDetails.add(billDetail);
		}

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

		List<EgBillPayeedetails> payeeDetails = new ArrayList<EgBillPayeedetails>();

		if (record.getNetPayableDetail() == null) {
			return payeeDetails;
		}

		ExpenseNetPayableRecord sourcePayee = record.getNetPayableDetail();
		/***
		 * call glocde api client to fetch data and set the id
		 */
//		reference.setGlcodeid(sourcePayee.ge);
		Accountdetailtype accountdetailtype = accountDetailTypeServiceClient.getByName(record.getSubLedgerType(),
				requestInfo, tenantId);

		EgBillPayeedetails billPayeedetails = new EgBillPayeedetails();

		billPayeedetails.setIsDebit(false);
		billPayeedetails.setCreditAmount(sourcePayee.getCreditAmount());
		billPayeedetails.setAccountDetailTypeId(accountdetailtype.getId().longValue());
		Accountdetailkey accountDetailKey = accountDetailKeyServiceClient.getAccountDetailKey(accountdetailtype.getId(),
				record.getSubLedgerMaster(), requestInfo, tenantId);
		billPayeedetails.setAccountDetailKeyId(accountDetailKey.getDetailkey().longValue());
		payeeDetails.add(billPayeedetails);

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
		AppConfigValue appConfigValue = new AppConfigValue();
		appConfigValue.setId(id);
		checkList.setAppconfigvalue(appConfigValue);
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
		 * Add all debit amounts
		 */
		if (record.getDebitDetails() != null) {
			for (ExpenseDebitRecord debitDetail : record.getDebitDetails()) {
				if (debitDetail.getDebitAmount() != null) {
					totalAmount = totalAmount.add(debitDetail.getDebitAmount());
				}
			}
		}

		/*
		 * Add all credit amounts
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