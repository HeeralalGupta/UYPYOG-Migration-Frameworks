package org.egov.finance.migration.service.validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class BankToBankTransferRowValidator implements MigrationRowValidator {

	private final DataFormatter formatter = new DataFormatter();

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/uuuu")
			.withResolverStyle(ResolverStyle.STRICT);

	private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");

	private static final int MAX_NARRATION_LENGTH = 500;

	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		/*
		 * ===================================================== 1. ULB NAME
		 * =====================================================
		 */

		validateRequired(row, headerMap, "ulbname", "ULB Name", validationError);

		/*
		 * ===================================================== 2. VOUCHER DATE
		 * =====================================================
		 */

		validateDate(row, headerMap, "voucherdate", "Voucher Date", validationError);

		/*
		 * ===================================================== 3. FROM FUND
		 * =====================================================
		 */

		validateRequired(row, headerMap, "fromfund", "From Fund", validationError);

		/*
		 * ===================================================== 4. FROM SUB SCHEME
		 * (OPTIONAL) =====================================================
		 */

		// Optional. Master existence is checked in the mapper.

		/*
		 * ===================================================== 5. FROM DEPARTMENT
		 * =====================================================
		 */

		validateRequired(row, headerMap, "fromdepartment", "From Department", validationError);

		/*
		 * ===================================================== 6. FUNCTION
		 * =====================================================
		 */

		validateRequired(row, headerMap, "function", "Function", validationError);

		/*
		 * ===================================================== 7. FROM BANK
		 * =====================================================
		 */

		validateRequired(row, headerMap, "frombank", "From Bank", validationError);

		/*
		 * ===================================================== 8. FROM ACCOUNT NUMBER
		 * =====================================================
		 */

		validateRequired(row, headerMap, "fromaccountno", "From Account Number", validationError);

		/*
		 * ===================================================== 9. TO FUND
		 * =====================================================
		 */

		validateRequired(row, headerMap, "tofund", "To Fund", validationError);

		/*
		 * ===================================================== 10. INTER-FUND TRANSFER
		 * DETECTION =====================================================
		 */

		String fromFund = getValue(row, headerMap, "fromfund");

		String toFund = getValue(row, headerMap, "tofund");

		boolean interFundTransfer = !fromFund.isEmpty() && !toFund.isEmpty() && !fromFund.equals(toFund);

		/*
		 * ===================================================== 11. TO DEPARTMENT
		 * =====================================================
		 */

		if (interFundTransfer) {

			validateRequired(row, headerMap, "todepartment", "To Department", validationError);
		}

		/*
		 * ===================================================== 12. TO BANK
		 * =====================================================
		 */

		validateRequired(row, headerMap, "tobank", "To Bank", validationError);

		/*
		 * ===================================================== 13. TO ACCOUNT NUMBER
		 * =====================================================
		 */

		validateRequired(row, headerMap, "toaccountno", "To Account Number", validationError);

		/*
		 * ===================================================== 14. FROM / TO ACCOUNT
		 * VALIDATION =====================================================
		 */

		String fromAccountNumber = getValue(row, headerMap, "fromaccountno");

		String toAccountNumber = getValue(row, headerMap, "toaccountno");

		if (!fromAccountNumber.isEmpty() && !toAccountNumber.isEmpty() && fromAccountNumber.equals(toAccountNumber)) {

			validationError.getErrors().add("From Account Number and To Account Number " + "must be different");
		}

		/*
		 * ===================================================== 15. SOURCE INTER FUND
		 * CODE =====================================================
		 */

		if (interFundTransfer) {

			validateRequired(row, headerMap, "sourceinterfundcode", "Source Inter Fund Code", validationError);
		}

		/*
		 * ===================================================== 16. DESTINATION INTER
		 * FUND CODE =====================================================
		 */

		if (interFundTransfer) {

			validateRequired(row, headerMap, "destinationinterfundcode", "Destination Inter Fund Code",
					validationError);
		}

		/*
		 * ===================================================== 17. MODE OF TRANSACTION
		 * =====================================================
		 */

		String modeOfTransaction = getValue(row, headerMap, "modeoftransaction");

		validateRequired(row, headerMap, "modeoftransaction", "Mode of Transaction", validationError);

		if (!modeOfTransaction.isEmpty() && !isValidTransactionMode(modeOfTransaction)) {

			validationError.getErrors().add("Mode of Transaction must be either " + "RTGS/NEFT or cheque");
		}

		/*
		 * ===================================================== 18. REFERENCE NUMBER
		 * =====================================================
		 */

		validateRequired(row, headerMap, "referencenumber", "Reference Number", validationError);

		/*
		 * ===================================================== 19. REFERENCE DATE
		 * =====================================================
		 */

		validateDate(row, headerMap, "referencedate", "Reference Date", validationError);

		/*
		 * ===================================================== 20. AMOUNT
		 * =====================================================
		 */

		String amount = getValue(row, headerMap, "amountrs");

		validateRequired(row, headerMap, "amountrs", "Amount", validationError);

		if (!amount.isEmpty()) {

			validateAmount(amount, validationError);
		}

		/*
		 * ===================================================== 21. NARRATION
		 * =====================================================
		 */

		String narration = getValue(row, headerMap, "narration");

		if (narration.length() > MAX_NARRATION_LENGTH) {

			validationError.getErrors().add("Narration cannot exceed 500 characters");
		}

		return validationError;
	}

	/**
	 * Validate mandatory field.
	 */
	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError result) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {

			result.getErrors().add(displayName + " is required");
		}
	}

	/**
	 * Get cell value using dynamic header mapping.
	 */
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

	/**
	 * Validate date format and future date restriction.
	 *
	 * Matches BankTransferMapper.parseAndValidateDate().
	 */
	private void validateDate(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError result) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {

			result.getErrors().add(displayName + " is required");

			return;
		}

		LocalDate parsedDate;

		try {

			parsedDate = LocalDate.parse(value, DATE_FORMATTER);

		} catch (DateTimeParseException e) {

			result.getErrors().add(displayName + " must be in dd/MM/yyyy format");

			return;
		}

		if (parsedDate.isAfter(LocalDate.now())) {

			result.getErrors().add(displayName + " cannot be in the future");
		}
	}

	/**
	 * Validate transaction mode.
	 *
	 * Exact values accepted by BankTransferRequest API.
	 */
	private boolean isValidTransactionMode(String mode) {

		return "RTGS/NEFT".equals(mode) || "cheque".equals(mode);
	}

	/**
	 * Validate amount.
	 */
	private void validateAmount(String amount, RowValidationError result) {

		try {

			BigDecimal amountValue = new BigDecimal(amount.replace(",", "").trim());

			if (amountValue.compareTo(MIN_AMOUNT) < 0) {

				result.getErrors().add("Amount must be greater than or equal to 0.01");
			}

		} catch (NumberFormatException e) {

			result.getErrors().add("Amount must be numeric");
		}
	}
}
