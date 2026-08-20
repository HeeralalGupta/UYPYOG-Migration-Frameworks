package org.egov.finance.migration.service.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class JournalVoucherRowValidator implements MigrationRowValidator {

	private final DataFormatter formatter = new DataFormatter();

	private static final String DATE_FORMAT = "dd/MM/yyyy";

	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		/*
		 * ===================================================== 1. GL CODE
		 * =====================================================
		 */

		validateRequired(row, headerMap, "glcode", "GLCode", validationError);

		/*
		 * ===================================================== 2. DEBIT AMOUNT
		 * =====================================================
		 */

		validateRequired(row, headerMap, "debitamount", "DebitAmount", validationError);

		/*
		 * ===================================================== 3. CREDIT AMOUNT
		 * =====================================================
		 */

		validateRequired(row, headerMap, "creditamount", "CreditAmount", validationError);

		/*
		 * ===================================================== 4. DATE
		 *
		 * Only validate when the row contains VoucherDate.
		 *
		 * Continuation rows are allowed to be blank.
		 * =====================================================
		 */

		String voucherDate = getValue(row, headerMap, "voucherdate");

		if (!voucherDate.isEmpty()) {

			if (!isValidDate(voucherDate)) {

				validationError.getErrors().add("VoucherDate must be in dd/MM/yyyy format");
			}
		}

		/*
		 * ===================================================== 5. DEBIT NUMERIC
		 * =====================================================
		 */

		String debit = getValue(row, headerMap, "debitamount");

		if (!debit.isEmpty() && !isNumeric(debit)) {

			validationError.getErrors().add("DebitAmount must be numeric");
		}

		/*
		 * ===================================================== 6. CREDIT NUMERIC
		 * =====================================================
		 */

		String credit = getValue(row, headerMap, "creditamount");

		if (!credit.isEmpty() && !isNumeric(credit)) {

			validationError.getErrors().add("CreditAmount must be numeric");
		}

		/*
		 * ===================================================== 7. DEBIT / CREDIT VALUE
		 * =====================================================
		 */

		if (isNumeric(debit) && isNumeric(credit)) {

			try {

				double debitAmount = Double.parseDouble(debit);

				double creditAmount = Double.parseDouble(credit);

				if (debitAmount < 0) {

					validationError.getErrors().add("DebitAmount cannot be negative");
				}

				if (creditAmount < 0) {

					validationError.getErrors().add("CreditAmount cannot be negative");
				}

				/*
				 * A line should normally have either debit or credit amount.
				 */

				if (debitAmount == 0 && creditAmount == 0) {

					validationError.getErrors().add("Either DebitAmount or CreditAmount must be greater than zero");
				}

				if (debitAmount > 0 && creditAmount > 0) {

					validationError.getErrors().add("DebitAmount and CreditAmount cannot both be greater than zero");
				}

			} catch (NumberFormatException e) {

				validationError.getErrors().add("Invalid debit/credit amount");
			}
		}

		/*
		 * ===================================================== RETURN
		 * =====================================================
		 */

		return validationError;
	}

	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError result) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {

			result.getErrors().add(displayName + " is required");
		}
	}

	private String getValue(Row row, Map<String, Integer> headerMap, String header) {

		Integer columnIndex = headerMap.get(header);

		if (columnIndex == null) {
			return "";
		}

		Cell cell = row.getCell(columnIndex);

		if (cell == null) {
			return "";
		}

		return formatter.formatCellValue(cell).trim();
	}

	private boolean isNumeric(String value) {

		if (value == null || value.trim().isEmpty()) {

			return false;
		}

		try {

			Double.parseDouble(value.replace(",", ""));

			return true;

		} catch (NumberFormatException e) {

			return false;
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
}