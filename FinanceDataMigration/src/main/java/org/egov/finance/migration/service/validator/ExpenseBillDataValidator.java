package org.egov.finance.migration.service.validator;

import org.egov.finance.migration.modules.expensebill.dto.EgBillChecklist;
import org.egov.finance.migration.modules.expensebill.dto.EgBillPayeedetails;
import org.egov.finance.migration.modules.expensebill.dto.EgBilldetails;
import org.egov.finance.migration.modules.expensebill.dto.EgBillregister;
import org.egov.finance.migration.modules.expensebill.dto.EgBillregistermis;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillCreateRequest;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRequest;

public class ExpenseBillDataValidator {

	public void validateExpenseBillRequest(ExpenseBillCreateRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("Unable to build ExpenseBillRequest: ExpenseBillCreateRequest is null.");
		}

		if (request.getExpenseBillRequest() == null) {
			throw new IllegalArgumentException("Unable to build ExpenseBillRequest: expenseBillRequest is null.");
		}

		ExpenseBillRequest expenseRequest = request.getExpenseBillRequest();

		if (expenseRequest.getEgBillregister() == null) {
			throw new IllegalArgumentException("Unable to build ExpenseBillRequest: egBillregister is null.");
		}

		EgBillregister billRegister = expenseRequest.getEgBillregister();

		if (billRegister.getBillamount() == null) {
			throw new IllegalArgumentException("Missing required field: billamount");
		}

//		if (billRegister.getBillnumber() == null || billRegister.getBillnumber().trim().isEmpty()) {
//			throw new IllegalArgumentException("Missing required field: billnumber");
//		}

//		if (billRegister.getBilldate() == null || billRegister.getBilldate().trim().isEmpty()) {
//			throw new IllegalArgumentException("Missing required field: egBillregister.billdate");
//		}

//		if (billRegister.getExpendituretype() == null || billRegister.getExpendituretype().trim().isEmpty()) {
//			throw new IllegalArgumentException("Missing required field: egBillregister.expendituretype");
//		}

		if (billRegister.getEgBillregistermis() == null) {
			throw new IllegalArgumentException("Missing required object: egBillregister.egBillregistermis");
		}

		EgBillregistermis mis = billRegister.getEgBillregistermis();

		if (mis.getFund() == null) {
			throw new IllegalArgumentException("Missing required field: Fund");
		}

		if (mis.getDepartmentcode() == null || mis.getDepartmentcode().trim().isEmpty()) {
			throw new IllegalArgumentException(
					"Missing required field: Department");
		}

		if (mis.getFunction() == null) {
			throw new IllegalArgumentException("Missing required field: Function");
		}

		if (mis.getEgBillSubType() == null) {
			throw new IllegalArgumentException(
					"Missing required field: BillSubType");
		}

		if (billRegister.getBillDetails() == null || billRegister.getBillDetails().isEmpty()) {
			throw new IllegalArgumentException("Missing required field: billDetails");
		}

		for (int i = 0; i < billRegister.getBillDetails().size(); i++) {

			EgBilldetails detail = billRegister.getBillDetails().get(i);

			if (detail == null) {
				throw new IllegalArgumentException("Missing required object: billDetails[" + i + "]");
			}

			if (detail.getGlcodeid() == null) {
				throw new IllegalArgumentException(
						"Missing required field: billDetails[" + i + "].glcodeid");
			}

			if (detail.getDebitamount() == null && detail.getCreditamount() == null) {
				throw new IllegalArgumentException(
						"Missing debitamount/creditamount: billDetails[" + i + "]");
			}
		}

		if (billRegister.getBillPayeedetails() == null || billRegister.getBillPayeedetails().isEmpty()) {
			throw new IllegalArgumentException("Missing required field: billPayeedetails");
		}

		for (int i = 0; i < billRegister.getBillPayeedetails().size(); i++) {

			EgBillPayeedetails payee = billRegister.getBillPayeedetails().get(i);

			if (payee == null) {
				throw new IllegalArgumentException(
						"Missing required object: billPayeedetails[" + i + "]");
			}

			if (payee.getEgBilldetailsId() == null) {
				throw new IllegalArgumentException(
						"Missing required field: Netpayable[" + i + "].egBilldetailsId (netpayble glcode)");
			}

			if (payee.getCreditAmount() == null && payee.getDebitAmount() == null) {
				throw new IllegalArgumentException(
						"Missing debitAmount/creditAmount: billPayeedetails[" + i + "]");
			}

			if (payee.getAccountDetailTypeId() == null) {
				throw new IllegalArgumentException(
						"Missing required field: SubLedger Details[" + i + "].Sub Ledger Type(AccountDetailTypeId)");
			}

			if (payee.getAccountDetailKeyId() == null) {
				throw new IllegalArgumentException(
						"Missing required field: SubLedger Details[" + i + "].Sub Ledger Master(AccountDetailKeyId)");
			}
		}

//		if (billRegister.getCheckLists() == null || billRegister.getCheckLists().isEmpty()) {
//			throw new IllegalArgumentException("Missing required field: egBillregister.checkLists");
//		}

//		for (int i = 0; i < billRegister.getCheckLists().size(); i++) {
//
//			EgBillChecklist checklist = billRegister.getCheckLists().get(i);
//
//			if (checklist == null) {
//				throw new IllegalArgumentException("Missing required object: egBillregister.checkLists[" + i + "]");
//			}
//
//			if (checklist.getAppconfigvalue() == null) {
//				throw new IllegalArgumentException(
//						"Missing required field: egBillregister.checkLists[" + i + "].appconfigvalue");
//			}
//
//			if (checklist.getChecklistvalue() == null || checklist.getChecklistvalue().trim().isEmpty()) {
//				throw new IllegalArgumentException(
//						"Missing required field: egBillregister.checkLists[" + i + "].checklistvalue");
//			}
//		}
	}
}
