package org.egov.finance.migration.service.validator;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class ExpenseBillRowValidator implements MigrationRowValidator {

	private final DataFormatter formatter = new DataFormatter();

	private static final String DATE_FORMAT = "dd/MM/yyyy";

	
	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		/*
		 * ========================================================= 1. BILL HEADER
		 * VALIDATION
		 *
		 * A new Expense Bill starts when SN is present.
		 *
		 * Continuation rows can have bill-level fields blank.
		 * =========================================================
		 */

		String sn = getValue(row, headerMap, "sn");

		boolean isBillHeaderRow = !sn.isEmpty();

		if (isBillHeaderRow) {

			validateRequired(row, headerMap, "sn", "SN", validationError);
			validateRequired(row, headerMap, "ulbname", "ULB Name", validationError);
			validateRequired(row, headerMap, "billdate", "Bill Date", validationError);
			validateRequired(row, headerMap, "fund", "Fund", validationError);
			validateRequired(row, headerMap, "department", "Department", validationError);
			validateRequired(row, headerMap, "function", "Function", validationError);
			validateRequired(row, headerMap, "partybillno", "Party Bill No", validationError);
			validateRequired(row, headerMap, "partybilldate", "Party Bill Date", validationError);
			validateRequired(row, headerMap, "billsubtype", "Bill SubType", validationError);
			validateRequired(row, headerMap, "subledgertype", "Category Type (SubLedger Type)", validationError);
			validateRequired(row, headerMap, "subledgermaster", "SubLedger Master", validationError);

			/*
			 * ===================================================== BILL DATE VALIDATION
			 * =====================================================
			 */

			String billDate = getValue(row, headerMap, "billdate");

			if (!billDate.isEmpty() && !isValidDate(billDate)) {

				validationError.getErrors().add("Bill Date must be in dd/MM/yyyy format");
			}

			/*
			 * ===================================================== PARTY BILL DATE
			 * VALIDATION =====================================================
			 */

			String partyBillDate = getValue(row, headerMap, "partybilldate");

			if (!partyBillDate.isEmpty() && !isValidDate(partyBillDate)) {

				validationError.getErrors().add("Party Bill Date must be in dd/MM/yyyy format");
			}
		}

		/*
		 * ========================================================= 2. DEBIT DETAILS
		 *
		 * O = Debit GL Code P = Debit Account Head Q = Debit Amount
		 *
		 * If any one is present, all three are required.
		 * =========================================================
		 */

		boolean hasDebitData = validateDebitDetails(row, headerMap, validationError);

		/*
		 * ========================================================= 3. DEDUCTION
		 * DETAILS
		 *
		 * R = Deduction GL Code S = Deduction Account Head T = Deduction Percentage U =
		 * Deduction Credit Amount
		 *
		 * Deduction Percentage is optional.
		 *
		 * If GL Code / Account Head / Amount is used, mandatory fields must be present.
		 * =========================================================
		 */

		boolean hasDeductionData = validateDeductionDetails(row, headerMap, validationError);

		/*
		 * ========================================================= 4. NET PAYABLE
		 * DETAILS
		 *
		 * V = Net Payable GL Code W = Net Payable Credit Amount
		 *
		 * If either is present, both are required.
		 * =========================================================
		 */

		boolean hasNetPayableData = validateNetPayableDetails(row, headerMap, validationError);

		/*
		 * ========================================================= 5. FINANCIAL DETAIL
		 * CHECK
		 *
		 * A new Expense Bill must contain at least one financial detail.
		 * =========================================================
		 */

		if (isBillHeaderRow && !hasDebitData && !hasDeductionData && !hasNetPayableData) {

			validationError.getErrors().add("Expense Bill must contain financial details");
		}

		return validationError;
	}

	/*
	 * ========================================================= DEBIT DETAILS
	 * =========================================================
	 */
	private boolean validateDebitDetails(Row row, Map<String, Integer> headerMap, RowValidationError validationError) {

		String glCode = getValue(row, headerMap, "debitglcode");

		String accountHead = getValue(row, headerMap, "debitaccounthead");

		String debitAmount = getValue(row, headerMap, "debitamount");

		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAccountHead = !accountHead.isEmpty();
		boolean hasAmount = !debitAmount.isEmpty();

		/*
		 * Entire debit section is empty.
		 */
		if (!hasGlCode && !hasAccountHead && !hasAmount) {

			return false;
		}

		/*
		 * GL Code
		 */
		if (!hasGlCode) {

			validationError.getErrors().add("Debit GL Code should not be blank");

		} else {

			validateGlCode(glCode, "Debit GL Code", validationError);
		}

		/*
		 * Account Head
		 */
		if (!hasAccountHead) {

			validationError.getErrors().add("Debit Account Head should not be blank");
		}

		/*
		 * Debit Amount
		 */
		if (!hasAmount) {

			validationError.getErrors().add("Debit Amount should not be blank");

		} else {

			if (!isNumeric(debitAmount)) {

				validationError.getErrors().add("Debit Amount must be numeric");

			} else {

				validateNonNegativeAmount(debitAmount, "Debit Amount", validationError);
			}
		}

		return true;
	}

	/*
	 * ========================================================= DEDUCTION DETAILS
	 * =========================================================
	 */
	private boolean validateDeductionDetails(Row row, Map<String, Integer> headerMap,
			RowValidationError validationError) {

		String glCode = getValue(row, headerMap, "deductionglcode");

		String accountHead = getValue(row, headerMap, "deductionaccounthead");

		String percentage = getValue(row, headerMap, "deductionpercentage");

		String creditAmount = getValue(row, headerMap, "deductioncreditamount");

		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAccountHead = !accountHead.isEmpty();
		boolean hasPercentage = !percentage.isEmpty();
		boolean hasAmount = !creditAmount.isEmpty();

		/*
		 * Entire deduction section is empty.
		 */
		if (!hasGlCode && !hasAccountHead && !hasPercentage && !hasAmount) {

			return false;
		}

		/*
		 * GL Code
		 */
		if (!hasGlCode) {

			validationError.getErrors().add("Deduction GL Code should not be blank");

		} else {

			validateGlCode(glCode, "Deduction GL Code", validationError);
		}

		/*
		 * Account Head
		 */
		if (!hasAccountHead) {

			validationError.getErrors().add("Deduction Account Head should not be blank");
		}

		/*
		 * Deduction Percentage
		 *
		 * Optional.
		 */
		if (hasPercentage) {

			if (!isNumeric(percentage)) {

				validationError.getErrors().add("Deduction Percentage must be numeric");

			} else {

				validatePercentage(percentage, validationError);
			}
		}

		/*
		 * Deduction Credit Amount
		 */
		if (!hasAmount) {

			validationError.getErrors().add("Deduction Credit Amount should not be blank");

		} else {

			if (!isNumeric(creditAmount)) {

				validationError.getErrors().add("Deduction Credit Amount must be numeric");

			} else {

				validateNonNegativeAmount(creditAmount, "Deduction Credit Amount", validationError);
			}
		}

		return true;
	}

	/*
	 * ========================================================= NET PAYABLE DETAILS
	 * =========================================================
	 */
	private boolean validateNetPayableDetails(Row row, Map<String, Integer> headerMap,
			RowValidationError validationError) {

		String glCode = getValue(row, headerMap, "netpayableglcode");

		/*
		 * IMPORTANT:
		 *
		 * FileValidationService uses:
		 *
		 * netpayableamount
		 */
		String amount = getValue(row, headerMap, "netpayableamount");

		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAmount = !amount.isEmpty();

		/*
		 * Entire net payable section is empty.
		 */
		if (!hasGlCode && !hasAmount) {

			return false;
		}

		/*
		 * GL Code
		 */
		if (!hasGlCode) {

			validationError.getErrors().add("Net Payable GL Code should not be blank");

		} else {

			validateGlCode(glCode, "Net Payable GL Code", validationError);
		}

		/*
		 * Credit Amount
		 */
		if (!hasAmount) {

			validationError.getErrors().add("Net Payable Credit Amount should not be blank");

		} else {

			if (!isNumeric(amount)) {

				validationError.getErrors().add("Net Payable Credit Amount must be numeric");

			} else {

				validateNonNegativeAmount(amount, "Net Payable Credit Amount", validationError);
			}
		}

		return true;
	}

	/*
	 * ========================================================= REQUIRED FIELD
	 * =========================================================
	 */
	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {

			validationError.getErrors().add(displayName + " is required");
		}
	}

	/*
	 * ========================================================= GL CODE VALIDATION
	 *
	 * Expense Bill requires exactly 10 digits.
	 * =========================================================
	 */
	private void validateGlCode(String glCode, String displayName, RowValidationError validationError) {

		if (!glCode.matches("\\d{10}")) {

			validationError.getErrors().add(displayName + " must contain exactly 10 digits");
		}
	}

	/*
	 * ========================================================= NUMERIC VALIDATION
	 * =========================================================
	 */
	private boolean isNumeric(String value) {

		if (value == null || value.trim().isEmpty()) {

			return false;
		}

		try {

			new BigDecimal(value.replace(",", "").trim());

			return true;

		} catch (NumberFormatException e) {

			return false;
		}
	}

	/*
	 * ========================================================= AMOUNT VALIDATION
	 * =========================================================
	 */
	private void validateNonNegativeAmount(String value, String fieldName, RowValidationError validationError) {

		try {

			BigDecimal amount = new BigDecimal(value.replace(",", "").trim());

			if (amount.compareTo(BigDecimal.ZERO) < 0) {

				validationError.getErrors().add(fieldName + " cannot be negative");
			}

		} catch (NumberFormatException e) {

			validationError.getErrors().add("Invalid " + fieldName);
		}
	}

	/*
	 * ========================================================= PERCENTAGE
	 * VALIDATION =========================================================
	 */
	private void validatePercentage(String value, RowValidationError validationError) {

		try {

			BigDecimal percentage = new BigDecimal(value.replace(",", "").trim());

			if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {

				validationError.getErrors().add("Deduction Percentage must be between 0 and 100");
			}

		} catch (NumberFormatException e) {

			validationError.getErrors().add("Invalid Deduction Percentage");
		}
	}

	/*
	 * ========================================================= DATE VALIDATION
	 * =========================================================
	 */
	private boolean isValidDate(String value) {

		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

		sdf.setLenient(false);

		try {

			sdf.parse(value);

			return true;

		} catch (ParseException e) {

			return false;
		}
	}

	/*
	 * ========================================================= GET CELL VALUE
	 * =========================================================
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