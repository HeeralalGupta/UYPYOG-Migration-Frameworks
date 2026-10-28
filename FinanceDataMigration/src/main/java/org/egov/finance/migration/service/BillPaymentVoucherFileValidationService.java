
package org.egov.finance.migration.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.BillPaymentVoucherRowValidator;
import org.springframework.stereotype.Component;

@Component
public class BillPaymentVoucherFileValidationService extends AbstractFileValidationService {

	private static final int HEADER_ROW_INDEX = 3; // Excel Row 4

	private final BillPaymentVoucherRowValidator rowValidator;

	public BillPaymentVoucherFileValidationService(BillPaymentVoucherRowValidator rowValidator) {

		super(rowValidator);
		this.rowValidator = rowValidator;
	}

	/*
	 * ========================================================= MODULE
	 * =========================================================
	 */

	@Override
	protected MigrationType getModuleCode() {
		return MigrationType.BILL_PAYMENT_VOUCHER;
	}

	/*
	 * ========================================================= SHEET
	 * =========================================================
	 */

	@Override
	protected Sheet getSheet(Workbook workbook) {

		if (workbook.getNumberOfSheets() == 0) {
			throw new IllegalArgumentException("Excel workbook does not contain any sheet.");
		}

		// Find the named sheet first.
		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

			Sheet sheet = workbook.getSheetAt(i);

			if ("Bill Payment Voucher".equalsIgnoreCase(sheet.getSheetName().trim())) {
				return sheet;
			}
		}

		// Allow a single-sheet workbook with a different name.
		if (workbook.getNumberOfSheets() == 1) {
			return workbook.getSheetAt(0);
		}

		throw new IllegalArgumentException("Excel sheet 'Bill Payment Voucher' not found.");
	}

	/*
	 * ========================================================= FIND HEADER ROW
	 *
	 * Excel Row 4 = Java index 3.
	 * =========================================================
	 */

	@Override
	protected int findHeaderRow(Sheet sheet) {

		if (sheet.getLastRowNum() < HEADER_ROW_INDEX) {
			return -1;
		}

		Row row = sheet.getRow(HEADER_ROW_INDEX);

		if (row == null) {
			return -1;
		}
		
		boolean hasUlb = false;
		boolean hasPaymentDate = false;
		boolean hasPaymentMode = false;
		boolean hasBillNumber = false;
		boolean hasPaymentAmount = false;

		for (Cell cell : row) {

			String value = normalize(cell.toString());

			if (value.equals("ulbname")) {
				hasUlb = true;
			}

			if (value.equals("paymentvoucherdate")) {
				hasPaymentDate = true;
			}

			if (value.equals("paymentmode")) {
				hasPaymentMode = true;
			}

			if (value.equals("billnumber")) {
				hasBillNumber = true;
			}

			if (value.equals("paymentamount")) {
				hasPaymentAmount = true;
			}
		}

		if (hasUlb && hasPaymentDate && hasPaymentMode && hasBillNumber && hasPaymentAmount) {
			return HEADER_ROW_INDEX;
		}

		return -1;
	}

	/*
	 * ========================================================= HEADER VALIDATION
	 * =========================================================
	 */

	@Override
	protected boolean validateHeaders(Map<String, Integer> headerMap, FileValidationResult result) {

		boolean valid = true;

		valid &= validateHeader(headerMap, "sno", "S.No.", result);
		
		valid &= validateHeader(headerMap, "vouchergroupid", "Voucher Group Id", result);

		valid &= validateHeader(headerMap, "ulbname", "ULB Name", result);

		valid &= validateHeader(headerMap, "paymentvoucherdate", "Payment Voucher Date", result);

		valid &= validateHeader(headerMap, "fund", "Fund", result);

		valid &= validateHeader(headerMap, "function", "Function", result);

		valid &= validateHeader(headerMap, "department", "Department", result);

		valid &= validateHeader(headerMap, "paymentmode", "Payment Mode", result);

		valid &= validateHeader(headerMap, "bankname", "Bank Name", result);

		valid &= validateHeader(headerMap, "bankbranchname", "Bank Branch Name", result);

		valid &= validateHeader(headerMap, "ifsccode", "IFSC Code", result);

		valid &= validateHeader(headerMap, "bankaccountnumber", "Bank Account Number", result);

		valid &= validateHeader(headerMap, "description", "Description", result);

		valid &= validateHeader(headerMap, "billnumber", "Bill Number", result);

		valid &= validateHeader(headerMap, "paymentamount", "Payment Amount", result);

		return valid;
	}

	/*
	 * ========================================================= CREATE HEADER MAP
	 *
	 * All Bill Payment Voucher column names are unique.
	 * =========================================================
	 */

	@Override
	protected Map<String, Integer> createHeaderMap(Row headerRow) {

		Map<String, Integer> headerMap = new HashMap<>();

		if (headerRow == null) {
			return headerMap;
		}

		String[] keys = { "sno", "vouchergroupid", "ulbname", "paymentvoucherdate", "fund", "function", "department", "paymentmode",
				"bankname", "bankbranchname", "ifsccode", "bankaccountnumber", "description", "billnumber",
				"paymentamount" };

		for (int i = 0; i < keys.length; i++) {

			Cell cell = headerRow.getCell(i);

			if (cell == null) {
				continue;
			}

			String actualHeader = normalize(cell.toString());

			if (actualHeader.equals(keys[i])) {
				headerMap.put(keys[i], i);
			}
		}

		return headerMap;
	}

	/*
	 * ========================================================= HEADER HELPER
	 * =========================================================
	 */

	private boolean validateHeader(Map<String, Integer> headerMap, String key, String displayName,
			FileValidationResult result) {

		if (!headerMap.containsKey(key)) {

			result.getErrors().add("Missing column: " + displayName);

			return false;
		}

		return true;
	}

	/*
	 * ========================================================= NORMALIZE
	 * =========================================================
	 */

	private String normalize(String value) {
	    if (value == null) {
	        return "";
	    }

	    return value.trim()
	            .replace("\u00A0", "")  // Remove non-breaking spaces
	            .toLowerCase()
	            .replaceAll("[^a-z0-9]", "");
	}
}
