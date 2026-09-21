package org.egov.finance.migration.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.ContractorBillRowValidator;
import org.springframework.stereotype.Component;

@Component
public class ContractorBillFileValidationService extends AbstractFileValidationService {

	private static final int HEADER_ROW_INDEX = 3; // Excel Row 4

	private final ContractorBillRowValidator contractorBillRowValidator;

	public ContractorBillFileValidationService(ContractorBillRowValidator contractorBillRowValidator) {

		super(contractorBillRowValidator);

		this.contractorBillRowValidator = contractorBillRowValidator;
	}

	/*
	 * ========================================================= MODULE
	 * =========================================================
	 */

	@Override
	protected MigrationType getModuleCode() {

		return MigrationType.CONTRACTOR_BILL;
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

		return workbook.getSheetAt(0);
	}

	/*
	 * ========================================================= FIND HEADER ROW
	 *
	 * Contractor Bill template: Row 4 = actual column headers Java index = 3
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

		boolean hasBillDate = false;

		boolean hasContractor = false;

		for (Cell cell : row) {

			String value = normalize(cell.toString());

			if (value.startsWith("ulbname")) {
				hasUlb = true;
			}

			if (value.startsWith("billdate")) {
				hasBillDate = true;
			}

			if (value.startsWith("contractor")) {
				hasContractor = true;
			}
		}

		if (hasUlb && hasBillDate && hasContractor) {

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

		valid &= validateHeader(headerMap, "sn", "SN.", result);

		valid &= validateHeader(headerMap, "ulbname", "ULB Name", result);

		valid &= validateHeader(headerMap, "billdate", "Bill Date", result);

		valid &= validateHeader(headerMap, "contractor", "Contractor", result);

		valid &= validateHeader(headerMap, "workorder", "Work Order", result);

		valid &= validateHeader(headerMap, "fund", "Fund", result);

		valid &= validateHeader(headerMap, "department", "Department", result);

		valid &= validateHeader(headerMap, "scheme", "Scheme", result);

		valid &= validateHeader(headerMap, "fundsource", "Fund Source", result);

		valid &= validateHeader(headerMap, "function", "Function", result);

		valid &= validateHeader(headerMap, "narration", "Narration", result);

		valid &= validateHeader(headerMap, "partybillno", "Party Bill No", result);

		valid &= validateHeader(headerMap, "partybilldate", "Party Bill Date", result);

		valid &= validateHeader(headerMap, "partybillamount", "Party Bill Amount", result);

		valid &= validateHeader(headerMap, "billtype", "Bill Type", result);

		/*
		 * Debit
		 */

		valid &= validateHeader(headerMap, "debitglcode", "Debit GL Code", result);

		valid &= validateHeader(headerMap, "debitaccounthead", "Debit Account Head", result);

		valid &= validateHeader(headerMap, "debitamount", "Debit Amount", result);

		/*
		 * Deduction
		 */

		valid &= validateHeader(headerMap, "deductionglcode", "Deduction GL Code", result);

		valid &= validateHeader(headerMap, "deductionaccounthead", "Deduction Account Head", result);

		valid &= validateHeader(headerMap, "deductionpercentage", "Deduction Percentage", result);

		valid &= validateHeader(headerMap, "creditamount", "Credit Amount", result);

		/*
		 * Net payable
		 */

		valid &= validateHeader(headerMap, "netpayableglcode", "Net Payable GL Code", result);

		valid &= validateHeader(headerMap, "netpayableamount", "Net Payable Credit Amount", result);

		return valid;
	}

	/*
	 * ========================================================= IMPORTANT:
	 *
	 * Contractor Bill has duplicate column names:
	 *
	 * GL Code -> 3 times Account Head -> 2 times Credit Amount -> 2 times
	 *
	 * So the generic createHeaderMap() from the parent class cannot be used
	 * directly because duplicate keys overwrite.
	 * =========================================================
	 */

	@Override
	protected Map<String, Integer> createHeaderMap(Row headerRow) {

		Map<String, Integer> headerMap = new HashMap<>();

		/*
		 * Excel column positions
		 *
		 * A = 0 B = 1 ... X = 23
		 */

		String[] keys = {

				"sn", // A
				"ulbname", // B
				"billdate", // C
				"contractor", // D
				"workorder", // E
				"fund", // F
				"department", // G
				"scheme", // H
				"fundsource", // I
				"function", // J
				"narration", // K
				"partybillno", // L
				"partybilldate", // M
				"partybillamount", // N
				"billtype", // O

				"debitglcode", // P
				"debitaccounthead", // Q
				"debitamount", // R

				"deductionglcode", // S
				"deductionaccounthead", // T
				"deductionpercentage", // U
				"creditamount", // V

				"netpayableglcode", // W
				"netpayableamount" // X
		};

		String[] expectedHeaders = {

				"sn", "ulbname", "billdate", "contractor", "workorder", "fund", "department", "scheme", "fundsource",
				"function", "narration", "partybillno", "partybilldate", "partybillamount", "billtype",

				"glcodeaccountcode", "accounthead", "debitamount",

				"glcodeaccountcode", "accounthead", "deductionpercentage", "creditamount",

				"glcodeaccountcode", "creditamount" };

		for (int i = 0; i < keys.length; i++) {

			Cell cell = headerRow.getCell(i);

			if (cell == null) {
				continue;
			}

			String actualHeader = normalize(cell.toString());

			if (actualHeader.startsWith(expectedHeaders[i])) {

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

		return value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}