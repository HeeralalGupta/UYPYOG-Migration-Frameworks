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
	private static final String DATE_FORMAT = "yyyy-MM-dd";

	@Override
	public RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap) {

		RowValidationError validationError = new RowValidationError(excelRowNumber);

		/*
		 * ====== 1. REQUIRED BASIC BILL FIELDS =======
		 */

		validateRequired(row, headerMap, "billnumber", "Bill Number", validationError);
		validateRequired(row, headerMap, "billdate", "Bill Date", validationError);
		validateRequired(row, headerMap, "expendituretype", "Expenditure Type", validationError);

		/*
		 * ========== 2. BILL DATE =====================
		 */

		String billDate = getValue(row, headerMap, "billdate");

		if (!billDate.isEmpty() && !isValidDate(billDate)) {
			validationError.getErrors().add("Bill Date must be in yyyy-MM-dd format");
		}

		/*
		 * ========= 3. OPTIONAL PARTY BILL DATE ========
		 */

		String partyBillDate = getValue(row, headerMap, "partybilldate");

		if (!partyBillDate.isEmpty() && !isValidDate(partyBillDate)) {
			validationError.getErrors().add("Party Bill Date must be in yyyy-MM-dd format");
		}

		/*
		 * ======== 4. REQUIRED MIS FIELDS ======================
		 */

		validateRequiredNumeric(row, headerMap, "fundid", "Fund ID", validationError);
		validateRequiredNumeric(row, headerMap, "functionid", "Function ID", validationError);
		validateRequiredNumeric(row, headerMap, "schemeid", "Scheme ID", validationError);
		validateRequiredNumeric(row, headerMap, "subschemeid", "Sub Scheme ID", validationError);
		validateRequired(row, headerMap, "departmentcode", "Department Code", validationError);
		validateRequired(row, headerMap, "payto", "Pay To", validationError);
		validateRequiredNumeric(row, headerMap, "egbillsubtypeid", "Bill Sub Type ID", validationError);

		/*
		 * =========== 5. DEBIT DETAIL
		 *
		 * Debit GL Code and Debit Amount are mandatory.
		 * =====================================================
		 */

		validateRequiredNumeric(row, headerMap, "debitglcodeid", "Debit GL Code ID", validationError);
		validateRequiredAmount(row, headerMap, "debitamount", "Debit Amount", validationError);

		/*
		 * ====== 6. CREDIT / DEDUCTION DETAILS Each GL Code and Amount must come
		 * together. =====================================================
		 */

		validateGlAndAmountPair(row, headerMap, "credit1glcodeid", "credit1amount", "Credit 1", validationError);
		validateGlAndAmountPair(row, headerMap, "credit2glcodeid", "credit2amount", "Credit 2", validationError);
		validateGlAndAmountPair(row, headerMap, "credit3glcodeid", "credit3amount", "Credit 3", validationError);

		/*
		 * ======== 7. NET PAYABLE DETAIL GL Code and Amount are mandatory.
		 * =====================================================
		 */

		validateRequiredNumeric(row, headerMap, "netpayableglcodeid", "Net Payable GL Code ID", validationError);
		validateRequiredAmount(row, headerMap, "netpayableamount", "Net Payable Amount", validationError);

		/*
		 * ======= 8. PAYEE / SUB LEDGER DETAILS
		 *
		 * These values are required for billPayeedetails.
		 * =====================================================
		 */

		validateRequiredNumeric(row, headerMap, "netpayabledetailtypeid", "Net Payable Detail Type ID",
				validationError);
		validateRequiredNumeric(row, headerMap, "netpayabledetailkeyid", "Net Payable Detail Key ID", validationError);

		/*
		 * ========= 9. CHECKLIST VALUES
		 *
		 * The template contains four checklist columns.
		 * =====================================================
		 */

		validateRequired(row, headerMap, "checklist67", "Checklist 67", validationError);
		validateRequired(row, headerMap, "checklist68", "Checklist 68", validationError);
		validateRequired(row, headerMap, "checklist69", "Checklist 69", validationError);
		validateRequired(row, headerMap, "checklist70", "Checklist 70", validationError);

		/*
		 * ========= 10. OPTIONAL BILL AMOUNT VALIDATION
		 *
		 * Bill amount is validated if provided. Your builder may also calculate it
		 * automatically. =========================================
		 */

		String billAmount = getValue(row, headerMap, "billamount");

		if (!billAmount.isEmpty()) {
			validateAmountValue(billAmount, "Bill Amount", validationError);
		}

		/*
		 * ========= 11. FINANCIAL BALANCE VALIDATION
		 *
		 * Debit Amount should equal:
		 *
		 * Credit1 + Credit2 + Credit3 + Net Payable
		 * =====================================================
		 */

		validateFinancialBalance(row, headerMap, validationError);
		return validationError;
	}

	/*
	 * ======== REQUIRED TEXT FIELD =================
	 */

	private void validateRequired(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {
			validationError.getErrors().add(displayName + " is required");
		}
	}

	/*
	 * ========= REQUIRED NUMERIC FIELD ============================
	 */

	private void validateRequiredNumeric(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {
			validationError.getErrors().add(displayName + " is required");
			return;
		}

		if (!isNumeric(value)) {
			validationError.getErrors().add(displayName + " must be numeric");
			return;
		}

		BigDecimal number = toBigDecimal(value);

		if (number.compareTo(BigDecimal.ZERO) <= 0) {
			validationError.getErrors().add(displayName + " must be greater than zero");
		}
	}

	/*
	 * ======= REQUIRED AMOUNT ===================
	 */

	private void validateRequiredAmount(Row row, Map<String, Integer> headerMap, String header, String displayName,
			RowValidationError validationError) {

		String value = getValue(row, headerMap, header);

		if (value.isEmpty()) {
			validationError.getErrors().add(displayName + " is required");
			return;
		}

		validateAmountValue(value, displayName, validationError);
	}

	/*
	 * ========= AMOUNT VALIDATION =========================
	 */

	private void validateAmountValue(String value, String displayName, RowValidationError validationError) {

		if (!isNumeric(value)) {
			validationError.getErrors().add(displayName + " must be numeric");
			return;
		}

		BigDecimal amount = toBigDecimal(value);

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			validationError.getErrors().add(displayName + " must be greater than zero");
		}
	}

	/*
	 * ========= GL CODE + AMOUNT PAIR VALIDATION
	 *
	 * Valid:
	 *
	 * GL = 1015 Amount = 1500
	 *
	 * Invalid:
	 *
	 * GL = 1015 Amount = blank
	 *
	 * OR
	 *
	 * GL = blank Amount = 1500 ===========================
	 */

	private void validateGlAndAmountPair(Row row, Map<String, Integer> headerMap, String glHeader, String amountHeader,
			String displayName, RowValidationError validationError) {
		String glCode = getValue(row, headerMap, glHeader);
		String amount = getValue(row, headerMap, amountHeader);
		boolean hasGlCode = !glCode.isEmpty();
		boolean hasAmount = !amount.isEmpty();
		/*
		 * Both are blank.
		 *
		 * Optional credit row, so allowed.
		 */

		if (!hasGlCode && !hasAmount) {
			return;
		}

		/*
		 * GL Code exists but amount is missing.
		 */

		if (hasGlCode && !hasAmount) {
			validationError.getErrors()
					.add(displayName + " Amount is required when " + displayName + " GL Code ID is provided");
			return;
		}

		/*
		 * Amount exists but GL Code is missing.
		 */

		if (!hasGlCode && hasAmount) {
			validationError.getErrors()
					.add(displayName + " GL Code ID is required when " + displayName + " Amount is provided");
			return;
		}

		/*
		 * GL Code validation.
		 */

		if (!isNumeric(glCode)) {
			validationError.getErrors().add(displayName + " GL Code ID must be numeric");
		}

		/*
		 * Amount validation.
		 */

		validateAmountValue(amount, displayName + " Amount", validationError);
	}

	/*
	 * ========== FINANCIAL BALANCE
	 *
	 * Debit =
	 *
	 * Credit 1 + Credit 2 + Credit 3 + Net Payable
	 * =====================================================
	 */

	private void validateFinancialBalance(Row row, Map<String, Integer> headerMap, RowValidationError validationError) {
		String debitValue = getValue(row, headerMap, "debitamount");
		String credit1Value = getValue(row, headerMap, "credit1amount");
		String credit2Value = getValue(row, headerMap, "credit2amount");
		String credit3Value = getValue(row, headerMap, "credit3amount");
		String netPayableValue = getValue(row, headerMap, "netpayableamount");

		/*
		 * Don't perform balance calculation if any mandatory amount is invalid.
		 */

		if (!isNumeric(debitValue) || !isNumeric(netPayableValue)) {
			return;
		}

		try {

			BigDecimal debit = toBigDecimal(debitValue);
			BigDecimal credit1 = getOptionalAmount(credit1Value);
			BigDecimal credit2 = getOptionalAmount(credit2Value);
			BigDecimal credit3 = getOptionalAmount(credit3Value);
			BigDecimal netPayable = toBigDecimal(netPayableValue);
			BigDecimal totalCredit = credit1.add(credit2).add(credit3).add(netPayable);

			if (debit.compareTo(totalCredit) != 0) {
				validationError.getErrors().add("Debit Amount must be equal to "
						+ "Credit Amount + Net Payable Amount. " + "Debit=" + debit + ", Total Credit=" + totalCredit);
			}

		} catch (Exception e) {
			validationError.getErrors().add("Unable to validate debit and credit balance");
		}
	}

	/*
	 * ======== OPTIONAL AMOUNT ===========================
	 */

	private BigDecimal getOptionalAmount(String value) {
		if (value == null || value.trim().isEmpty()) {
			return BigDecimal.ZERO;
		}

		if (!isNumeric(value)) {
			return BigDecimal.ZERO;
		}
		return toBigDecimal(value);
	}

	/*
	 * ========== GET CELL VALUE ===========================
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

	/*
	 * ======= NUMERIC CHECK =================
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

	private BigDecimal toBigDecimal(String value) {
		return new BigDecimal(value.replace(",", "").trim());
	}

	/*
	 * ======== DATE VALIDATION ==================
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
}