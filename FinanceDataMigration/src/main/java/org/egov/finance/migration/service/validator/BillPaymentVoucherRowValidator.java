
package org.egov.finance.migration.service.validator;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class BillPaymentVoucherRowValidator implements MigrationRowValidator {

	private final DataFormatter formatter = new DataFormatter();

	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		if (row == null) {
			validationError.getErrors().add("Excel row is empty.");
			return validationError;
		}

		/*
		 * 1. Identify a payment voucher row.
		 *
		 * Each non-empty data row represents one bill belonging to a payment voucher.
		 * The reader groups related rows.
		 */
		boolean isDataRow = hasAnyValue(row, headerMap);

		if (!isDataRow) {
			return validationError;
		}

		/*
		 * 2. Required voucher fields.
		 */
		validateRequired(row, headerMap, "ulbname", "ULB Name", validationError);

		validateRequired(row, headerMap, "paymentvoucherdate", "Payment Voucher Date", validationError);

		validateRequired(row, headerMap, "fund", "Fund", validationError);

		validateRequired(row, headerMap, "function", "Function", validationError);

		validateRequired(row, headerMap, "department", "Department", validationError);

		validateRequired(row, headerMap, "paymentmode", "Payment Mode", validationError);

		validateRequired(row, headerMap, "bankname", "Bank Name", validationError);

		validateRequired(row, headerMap, "bankbranchname", "Bank Branch Name", validationError);

		validateRequired(row, headerMap, "ifsccode", "IFSC Code", validationError);

		validateRequired(row, headerMap, "bankaccountnumber", "Bank Account Number", validationError);

		/*
		 * 3. Validate payment voucher date.
		 *
		 * Expected format: dd/MM/yyyy
		 */
		String paymentDate = getValue(row, headerMap, "paymentvoucherdate");

		if (!paymentDate.isEmpty() && !paymentDate.matches("^(0[1-9]|[12][0-9]|3[01])/" + "(0[1-9]|1[0-2])/\\d{4}$")) {

			validationError.getErrors().add("Payment Voucher Date must be in dd/MM/yyyy format.");
		}

		/*
		 * 4. Validate payment mode.
		 *
		 * These are the modes supported by PaymentCreateRequest.
		 */
		String paymentMode = getValue(row, headerMap, "paymentmode");

		if (!paymentMode.isEmpty() && !paymentMode.matches("(?i)^(CHEQUE|NEFT|RTGS)$")) {

			validationError.getErrors().add("Payment Mode must be CHEQUE, NEFT, or RTGS.");
		}

		/*
		 * 5. Validate bank account number.
		 */
		String accountNumber = getValue(row, headerMap, "bankaccountnumber");

		if (!accountNumber.isEmpty() && accountNumber.length() > 50) {

			validationError.getErrors().add("Bank Account Number must not exceed 50 characters.");
		}

		/*
		 * 6. Validate IFSC code format.
		 *
		 * Standard Indian IFSC structure: 4 letters + 0 + 6 alphanumeric characters.
		 */
		String ifscCode = getValue(row, headerMap, "ifsccode");

		if (!ifscCode.isEmpty() && !ifscCode.matches("(?i)^[A-Z]{4}0[A-Z0-9]{6}$")) {

			validationError.getErrors().add("IFSC Code must contain 11 characters in a valid format.");
		}

		/*
		 * 7. Validate bill number.
		 */
		validateRequired(row, headerMap, "billnumber", "Bill Number", validationError);

		String billNumber = getValue(row, headerMap, "billnumber");

		if (!billNumber.isEmpty() && billNumber.length() > 100) {
			validationError.getErrors().add("Bill Number must not exceed 100 characters.");
		}

		/*
		 * 8. Validate payment amount.
		 *
		 * Amount must be numeric, greater than zero, with no more than 15 integer
		 * digits and 2 decimals.
		 */
		String paymentAmount = getValue(row, headerMap, "paymentamount");

		if (paymentAmount.isEmpty()) {

			validationError.getErrors().add("Payment Amount should not be blank.");

		} else {

			validatePaymentAmount(paymentAmount, validationError);
		}

		/*
		 * 9. Validate description length. Description is optional.
		 */
		String description = getValue(row, headerMap, "description");

		if (description.length() > 1000) {
			validationError.getErrors().add("Description must not exceed 1000 characters.");
		}

		return validationError;
	}

	/*
	 * REQUIRED FIELD VALIDATION
	 */
	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {
			validationError.getErrors().add(displayName + " should not be blank.");
		}
	}

	/*
	 * PAYMENT AMOUNT VALIDATION
	 */
	private void validatePaymentAmount(String amount, RowValidationError validationError) {

		try {
			// Remove common thousands separators.
			String normalizedAmount = amount.replace(",", "").trim();

			if (!normalizedAmount.matches("^\\d{1,15}(\\.\\d{1,2})?$")) {

				validationError.getErrors()
						.add("Payment Amount must be numeric with " + "up to 15 integer digits and 2 decimal places.");
				return;
			}

			BigDecimal value = new BigDecimal(normalizedAmount);

			if (value.compareTo(BigDecimal.ZERO) <= 0) {
				validationError.getErrors().add("Payment Amount must be greater than zero.");
			}

		} catch (NumberFormatException e) {

			validationError.getErrors().add("Payment Amount must be a valid number.");
		}
	}

	/*
	 * CHECK WHETHER A ROW CONTAINS ANY DATA
	 */
	private boolean hasAnyValue(Row row, Map<String, Integer> headerMap) {

		for (Integer columnIndex : headerMap.values()) {

			Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

			if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
				return true;
			}
		}

		return false;
	}

	/*
	 * GET CELL VALUE
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
