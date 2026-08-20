package org.egov.finance.migration.common.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MigrationColumnConfig {

	private static final Map<String, List<String>> REQUIRED_COLUMNS = new HashMap<String, List<String>>();

	static {
		
		/*
		 * FUND
		 */
		REQUIRED_COLUMNS.put("FUND", Arrays.asList("Sl. No.", "ULB Name", "Fund Name", "Nature of Fund", "Parent Fund"));
		
		/*
		 * SCHEME
		 */
		REQUIRED_COLUMNS.put("SCHEME", Arrays.asList("Sl. No.", "ULB Name", "Scheme Name", "Fund", "Status", "Start Date", "End Date", "Description"));

		/*
		 * BANK
		 *
		 * IMPORTANT: Replace these names with the exact headers present in your BANK
		 * template.
		 */
		REQUIRED_COLUMNS.put("BANK", Arrays.asList("Sl. No.", "ULB Name", "Bank Name", "Narration"));

		/*
		 * BANK_BRANCH
		 */
		REQUIRED_COLUMNS.put("BANK_BRANCH", Arrays.asList("Sl. No.", "ULB Name", "Bank", "Branch Name/Location", "IFSC Code", "Branch Code", "MICR", "Address", "Contact Person", "Phone Number", "Narration"));

		/*
		 * BANK_ACCOUNT
		 */
		REQUIRED_COLUMNS.put("BANK_ACCOUNT", Arrays.asList("Sl. No.", "ULB Name", "Bank Branch", "IFSC Code", "Account Number", "Fund", "Account Type", "Description", "Pay To", "Usage Type"));
		
		/*
		 * CONTRACTOR
		 */
		REQUIRED_COLUMNS.put("CONTRACTOR", Arrays.asList("Sl. No.", "ULB Name", "Contractor Name", "Correspondence Address", "Permanent Address", "Contact Person", "Mobile Number", "Email", "Narration", "GST Number", "GST Registered Status", "Bank Name", "Bank Branch", "IFSC Code", "Bank Account Number", "Contractor Type", "Source", "Registration Number", "Status", "PAN Number", "EPF Number", "ESI Number"));
		
		/*
		 * SUPPLIER
		 */
		REQUIRED_COLUMNS.put("SUPPLIER", Arrays.asList("Sl. No.", "ULB Name", "Supplier Name", "Correspondence Address", "Permanent Address", "Contact Person", "Mobile Number", "Email", "Narration", "GST Number", "GST Registered Status", "Bank Name", "Bank Branch", "IFSC Code", "Bank Account Number", "Supplier Type", "Source", "Registration Number", "Status", "PAN Number", "EPF Number", "ESI Number"));
		
		/*
		 * WORK
		 */
		REQUIRED_COLUMNS.put("WORK", Arrays.asList("Sl. No.", "ULB Name", "Name of Work", "Work Type", "Fund", "Estimate Value (₹)", "Start Date", "End Date"));
		
		/*
		 * WORK ORDER
		 */
		REQUIRED_COLUMNS.put("WORK_ORDER", Arrays.asList("Sl. No.", "ULB Name", "Tender Number", "Work Order No.", "Work Order Date", "Work Order Name", "Work Order Type", "Description", "Active", "Contractor Name", "Work Name", "Total Order Amt", "Advance Payable", "Fund", "Department", "Scheme", "Sub Scheme", "Work Order Issuing Authority", "Sanction Date", "EMD Amount", "BG Amount", "APBG Amount"));
		
		/*
		 * PURCHASE ORDER
		 */
		REQUIRED_COLUMNS.put("PURCHASE_ORDER", Arrays.asList("Sl. No.", "ULB Name", "Order No", "Order Date", "Order Name", "Description", "Supplier Name", "Source of Fund", "Department", "Scheme", "Sub Scheme", "Sanction No.", "Sanction Date", "Advance Payable (₹)", "Total Order Value (₹)"));
		
		/*
		 * CONTRACTOR BILL
		 */
		REQUIRED_COLUMNS.put("CONTRACTOR_BILL", Arrays.asList("SN.", "ULB Name", "Bill Date (dd/mm/yyyy)", "Contractor", "Work Order", "Fund", "Department", "Scheme", "Fund Source", "Function", "Narration", "Party Bill No", "Party Bill Date (dd/mm/yyyy)", "Party Bill Amount", "Bill Type", "GL Code (Account Code)", "Account Head", "Debit amount", "GL Code (Account Code)", "Account Head", "Deduction Percentage", "Credit amount", "GL Code(Account Code)", "Credit Amount"));
		
		/*
		 * EXPENSE BILL
		 */
		REQUIRED_COLUMNS.put("EXPENSE_BILL", Arrays.asList("SN.", "ULB Name", "Bill Date (dd/mm/yyyy)", "Fund", "Department", "Scheme", "Fund Source", "Function", "Narration", "Party Bill No", "Party Bill Date (dd/mm/yyyy)", "Bill SubType", "Category Type (SubLedger Type)", "SubLedger Master", "GL Code (Account Code)", "Account Head", "Debit amount", "GL Code (Account Code)", "Account Head", "Deduction Percentage", "Credit amount", "GL Code(Account Code)", "Credit Amount"));
		
		/*
		 * SUPPLIER BILL
		 */
		REQUIRED_COLUMNS.put("SUPPLIER_BILL", Arrays.asList("SN.", "ULB Name", "Bill Date (dd/mm/yyyy)", "Supplier", "Purchase Order", "Fund", "Department", "Scheme", "Sub Scheme", "Fund Source", "Function", "Narration", "Party Bill No", "Party Bill Date (dd/mm/yyyy)", "Bill Type", "GL Code (Account Code)", "Account Head", "Debit amount", "GL Code (Account Code)", "Account Head", "Deduction Percentage", "Credit amount", "GL Code(Account Code)", "Credit Amount"));
		/*
		 * JOURNAL_VOUCHER
		 *
		 * Put the exact headers from your JV template here.
		 */
		REQUIRED_COLUMNS.put("JOURNAL_VOUCHER", Arrays.asList("S.No.", "ULB Name", "VoucherDate", "VoucherName", "VoucherType", "Department", "Fund", "Function", "Scheme", "SubScheme", "Source", "Description", "ServiceName", "GLCode", "DebitAmount", "CreditAmount", "SubledgerFunctionCode(SubledgerDetail)", "SubledgerDetailType(Type)/ Subledger Category", "SubledgerDetailKey (Code)/ Subledger Master", "SubledgerAmount"));
		
	}

	private MigrationColumnConfig() {
	}

	public static List<String> getRequiredColumns(String moduleCode) {

		List<String> columns = REQUIRED_COLUMNS.get(moduleCode);

		if (columns == null) {
			return Collections.emptyList();
		}

		return columns;
	}
}
