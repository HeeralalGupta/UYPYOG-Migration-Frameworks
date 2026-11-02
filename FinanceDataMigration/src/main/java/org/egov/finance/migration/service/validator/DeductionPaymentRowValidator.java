package org.egov.finance.migration.service.validator;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class DeductionPaymentRowValidator implements MigrationRowValidator {

	private final DataFormatter formatter = new DataFormatter();

	private static final String DATE_FORMAT = "dd/MM/yyyy";

	private static final List<String> ALLOWED_PAYMENT_MODES = Arrays.asList("CHEQUE", "CASH", "RTGS", "NEFT");

	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		if (row == null) {
			validationError.getErrors().add("Excel row is empty.");
			return validationError;
		}

		/*
		 * ========================================================= 1. BASIC DETAILS
		 * =========================================================
		 */

		validateRequired(row, headerMap, "sn", "SL No.", validationError);
		validateRequired(row, headerMap, "ulbname", "ULB Name", validationError);

		/*
		 * ========================================================= 2. REFERENCE
		 * VOUCHER DETAILS =========================================================
		 */

		validateRequired(row, headerMap, "voucherdate", "Voucher Date", validationError);

		validateRequired(row, headerMap, "vouchernumber", "Voucher Number", validationError);

		validateRequired(row, headerMap, "vouchertype", "Voucher Type", validationError);

		/*
		 * ========================================================= 3. RECOVERY AND
		 * FUND =========================================================
		 */

		validateRequired(row, headerMap, "recoveryname", "Recovery Name", validationError);

		validateRequired(row, headerMap, "fund", "Fund", validationError);

		/*
		 * ========================================================= 4. DEDUCTION AMOUNT
		 * =========================================================
		 */

		validateRequired(row, headerMap, "deductionamount", "Deduction Amount", validationError);

		validateAmount(row, headerMap, validationError);

		/*
		 * ========================================================= 5. PAYMENT DATE
		 * =========================================================
		 */

		validateRequired(row, headerMap, "paymentdate", "Payment Date", validationError);

		validateDate(row, headerMap, "paymentdate", "Payment Date", validationError);

		/*
		 * ========================================================= 6. BANK DETAILS
		 * =========================================================
		 */

		validateRequired(row, headerMap, "bank", "Bank", validationError);

		validateRequired(row, headerMap, "branch", "Branch", validationError);

		validateRequired(row, headerMap, "bankaccountnumber", "Bank Account Number", validationError);

		/*
		 * ========================================================= 7. MODE OF PAYMENT
		 * =========================================================
		 */

		validateRequired(row, headerMap, "modeofpayment", "Mode Of Payment", validationError);

		validatePaymentMode(row, headerMap, validationError);

		/*
		 * ========================================================= 8. DEPARTMENT AND
		 * FUNCTION =========================================================
		 */
//
//		validateRequired(row, headerMap, "department", "Department", validationError);
//
//		validateRequired(row, headerMap, "function", "Function", validationError);

		return validationError;
	}

	/*
	 * ========================================================= REQUIRED FIELD
	 * VALIDATION =========================================================
	 */
	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {
			validationError.getErrors().add(displayName + " is required.");
		}
	}

	/*
	 * ========================================================= DATE VALIDATION
	 *
	 * Expected text format: dd/MM/yyyy Excel date-formatted cells are also
	 * supported. =========================================================
	 */
	private void validateDate(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		Integer columnIndex = headerMap.get(header);

		if (columnIndex == null) {
			return;
		}

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return;
		}

		String value = formatter.formatCellValue(cell).trim();

		if (value.isEmpty()) {
			return;
		}

		/*
		 * Excel stores date-formatted cells as numeric values. Validate those using
		 * Apache POI's date formatting metadata.
		 */
		if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

			if (!org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
				validationError.getErrors().add(displayName + " must be a valid Excel date or use dd/MM/yyyy format.");
			}

			return;
		}

		if (!isValidDate(value)) {
			validationError.getErrors().add(displayName + " must be in dd/MM/yyyy format.");
		}
	}

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
	 * ========================================================= AMOUNT VALIDATION
	 * =========================================================
	 */
	private void validateAmount(Row row, Map<String, Integer> headerMap, RowValidationError validationError) {

		String value = getValue(row, headerMap, "deductionamount");

		if (value.isEmpty()) {
			return;
		}

		BigDecimal amount;

		try {
			amount = new BigDecimal(value.replace(",", "").trim());
		} catch (NumberFormatException e) {
			validationError.getErrors().add("Deduction Amount must be numeric.");
			return;
		}

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			validationError.getErrors().add("Deduction Amount must be greater than zero.");
		}
	}

	/*
	 * ========================================================= PAYMENT MODE
	 * VALIDATION =========================================================
	 */
	private void validatePaymentMode(Row row, Map<String, Integer> headerMap, RowValidationError validationError) {

		String mode = getValue(row, headerMap, "modeofpayment");

		if (mode.isEmpty()) {
			return;
		}

		if (!ALLOWED_PAYMENT_MODES.contains(mode.trim().toUpperCase())) {
			validationError.getErrors()
					.add("Invalid Mode Of Payment: " + mode + ". Allowed values are: " + ALLOWED_PAYMENT_MODES);
		}
	}

	/*
	 * ========================================================= CELL VALUE HELPER
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
