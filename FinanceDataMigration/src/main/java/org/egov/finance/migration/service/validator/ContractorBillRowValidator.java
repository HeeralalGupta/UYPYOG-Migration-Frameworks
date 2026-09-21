package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class ContractorBillRowValidator implements MigrationRowValidator {

	private final DataFormatter formatter = new DataFormatter();

	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		/*
		 * ========================================================= 1. BILL HEADER
		 * VALIDATION
		 *
		 * Only validate these fields when the row starts a new bill.
		 *
		 * In Contractor Bill template: Row 5 -> bill starts Row 6-8 -> continuation
		 * debit rows Row 9 -> next bill
		 * =========================================================
		 */

		boolean isBillHeaderRow = hasValue(row, headerMap, "ulbname");

		if (isBillHeaderRow) {

			validateRequired(row, headerMap, "ulbname", "ULB Name", validationError);
			validateRequired(row, headerMap, "billdate", "Bill Date", validationError);
			validateRequired(row, headerMap, "contractor", "Contractor", validationError);
			validateRequired(row, headerMap, "workorder", "Work Order", validationError);
			validateRequired(row, headerMap, "fund", "Fund", validationError);
			validateRequired(row, headerMap, "department", "Department", validationError);
			validateRequired(row, headerMap, "function", "Function", validationError);
			validateRequired(row, headerMap, "partybillno", "Party Bill No", validationError);
			validateRequired(row, headerMap, "partybilldate", "Party Bill Date", validationError);
			validateRequired(row, headerMap, "partybillamount", "Party Bill Amount", validationError);
			validateRequired(row, headerMap, "billtype", "Bill Type", validationError);
		}

		/*
		 * ========================================================= 2. DEBIT DETAILS
		 *
		 * P = Debit GL Code Q = Account Head R = Debit Amount
		 *
		 * If any one is entered, all three are required.
		 * =========================================================
		 */

		validateDebitDetails(row, headerMap, validationError);

		/*
		 * ========================================================= 3. DEDUCTION /
		 * CREDIT DETAILS
		 *
		 * S = GL Code T = Account Head U = Deduction Percentage (OPTIONAL) V = Credit
		 * Amount
		 *
		 * If S/T/V is used, all mandatory fields are required.
		 * =========================================================
		 */

		validateDeductionDetails(row, headerMap, validationError);

		/*
		 * ========================================================= 4. NET PAYABLE
		 *
		 * W = GL Code X = Credit Amount
		 *
		 * If either is present, both are required.
		 * =========================================================
		 */

		validateNetPayableDetails(row, headerMap, validationError);

		return validationError;
	}

	/*
	 * ============================================================= REQUIRED FIELD
	 * =============================================================
	 */

	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {
			validationError.getErrors().add(displayName + " should not be blank");
		}
	}

	/*
	 * ============================================================= DEBIT DETAILS
	 * =============================================================
	 */

	private void validateDebitDetails(Row row, Map<String, Integer> headerMap, RowValidationError validationError) {

		String glCode = getValue(row, headerMap, "debitglcode");

		String accountHead = getValue(row, headerMap, "debitaccounthead");

		String debitAmount = getValue(row, headerMap, "debitamount");

		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAccountHead = !accountHead.isEmpty();
		boolean hasAmount = !debitAmount.isEmpty();

		// Completely empty debit section -> allowed
		if (!hasGlCode && !hasAccountHead && !hasAmount) {
			return;
		}

		if (!hasGlCode) {
			validationError.getErrors().add("Debit GL Code should not be blank");
		} else {
			validateGlCode(glCode, "Debit GL Code", validationError);
		}

		if (!hasAccountHead) {
			validationError.getErrors().add("Debit Account Head should not be blank");
		}

		if (!hasAmount) {
			validationError.getErrors().add("Debit Amount should not be blank");
		}
	}

	/*
	 * ============================================================= DEDUCTION /
	 * CREDIT DETAILS =============================================================
	 */

	private void validateDeductionDetails(Row row, Map<String, Integer> headerMap, RowValidationError validationError) {

		String glCode = getValue(row, headerMap, "deductionglcode");

		String accountHead = getValue(row, headerMap, "deductionaccounthead");

		String creditAmount = getValue(row, headerMap, "creditamount");

		/*
		 * Deduction Percentage is optional.
		 */
		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAccountHead = !accountHead.isEmpty();
		boolean hasAmount = !creditAmount.isEmpty();

		// Entire deduction section empty -> allowed
		if (!hasGlCode && !hasAccountHead && !hasAmount) {
			return;
		}

		if (!hasGlCode) {
			validationError.getErrors().add("Deduction GL Code should not be blank");
		} else {
			validateGlCode(glCode, "Deduction GL Code", validationError);
		}

		if (!hasAccountHead) {
			validationError.getErrors().add("Deduction Account Head should not be blank");
		}

		if (!hasAmount) {
			validationError.getErrors().add("Deduction Credit Amount should not be blank");
		}
	}

	/*
	 * ============================================================= NET PAYABLE
	 * DETAILS =============================================================
	 */

	private void validateNetPayableDetails(Row row, Map<String, Integer> headerMap,
			RowValidationError validationError) {

		String glCode = getValue(row, headerMap, "netpayableglcode");

		String amount = getValue(row, headerMap, "netpayableamount");

		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAmount = !amount.isEmpty();

		// Entire section empty -> allowed
		if (!hasGlCode && !hasAmount) {
			return;
		}

		if (!hasGlCode) {
			validationError.getErrors().add("Net Payable GL Code should not be blank");
		} else {
			validateGlCode(glCode, "Net Payable GL Code", validationError);
		}

		if (!hasAmount) {
			validationError.getErrors().add("Net Payable Credit Amount should not be blank");
		}
	}

	/*
	 * ============================================================= GL CODE
	 * VALIDATION
	 *
	 * Contractor Bill template requires exactly 10 digits.
	 * =============================================================
	 */

	private void validateGlCode(String glCode, String displayName, RowValidationError validationError) {

		if (!glCode.matches("\\d{10}")) {
			validationError.getErrors().add(displayName + " must contain exactly 10 digits");
		}
	}

	/*
	 * ============================================================= CHECK WHETHER A
	 * CELL HAS VALUE =============================================================
	 */

	private boolean hasValue(Row row, Map<String, Integer> headerMap, String header) {

		return !getValue(row, headerMap, header).isEmpty();
	}

	/*
	 * ============================================================= GET CELL VALUE
	 * =============================================================
	 */

	private String getValue(Row row, Map<String, Integer> headerMap, String header) {

		Integer columnIndex = headerMap.get(header);

		if (columnIndex == null) {
			return "";
		}

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return "";
		}

		return formatter.formatCellValue(cell).trim();
	}
}