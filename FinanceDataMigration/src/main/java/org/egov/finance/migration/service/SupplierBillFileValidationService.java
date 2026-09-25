package org.egov.finance.migration.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.SupplierBillRowValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class SupplierBillFileValidationService extends AbstractFileValidationService {

	/**
	 * Actual Excel header row:
	 *
	 * Excel Row 7 Java index = 6
	 */
	private static final int HEADER_ROW_INDEX = 6;

	private final SupplierBillRowValidator supplierBillRowValidator;

	public SupplierBillFileValidationService(SupplierBillRowValidator supplierBillRowValidator) {

		super(supplierBillRowValidator);
		this.supplierBillRowValidator = supplierBillRowValidator;
	}

	@Override
	public FileValidationResult validate(MultipartFile file) {

		FileValidationResult result = new FileValidationResult();

		result.setFileName(file.getOriginalFilename());

		result.setModuleCode(getModuleCode().toString());

		try (InputStream inputStream = file.getInputStream();

				Workbook workbook = WorkbookFactory.create(inputStream)) {

			/*
			 * ===================================================== 1. FORMULA EVALUATOR
			 *
			 * This allows validation of cells containing formulas.
			 * =====================================================
			 */
			FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

			/*
			 * ===================================================== 2. GET SUPPLIER BILL
			 * SHEET =====================================================
			 */
			Sheet sheet = getSheet(workbook);

			if (sheet == null) {

				result.setValid(false);

				result.getErrors().add("Supplier Bill sheet not found.");

				return result;
			}

			/*
			 * ===================================================== 3. FIND HEADER ROW
			 *
			 * Supplier Bill:
			 *
			 * Excel Row 7 Java index 6
			 * =====================================================
			 */
			int headerRowIndex = findHeaderRow(sheet);

			if (headerRowIndex == -1) {

				result.setValid(false);

				result.getErrors()
						.add("Header row & column not found in the Excel sheet. "
								+ "Please update the Excel sheet with proper headers "
								+ "and mandatory columns and Try again.");

				return result;
			}

			/*
			 * ===================================================== 4. HEADER INFORMATION
			 * =====================================================
			 */

			/*
			 * Excel row number starts from 1.
			 */
			result.setHeaderRow(headerRowIndex + 1);

			result.setHeaderStartRow(headerRowIndex + 1);

			result.setHeaderEndRow(headerRowIndex + 1);

			/*
			 * ===================================================== 5. CREATE HEADER MAP
			 *
			 * Important:
			 *
			 * Supplier Bill contains duplicate headers:
			 *
			 * GL Code -> 3 times Account Head -> 2 times Credit Amount -> 2 times
			 *
			 * Therefore SupplierBillFileValidationService overrides createHeaderMap().
			 * =====================================================
			 */
			Row headerRow = sheet.getRow(headerRowIndex);

			Map<String, Integer> headerMap = createHeaderMap(headerRow);

			result.setColumnCount(headerMap.size());

			/*
			 * ===================================================== 6. VALIDATE HEADERS
			 * =====================================================
			 */
			if (!validateHeaders(headerMap, result)) {

				result.setValid(false);

				return result;
			}

			/*
			 * ===================================================== 7. DATA START ROW
			 *
			 * Header = Excel Row 7 Data = Excel Row 8
			 * =====================================================
			 */
			result.setDataStartRow(headerRowIndex + 2);

			/*
			 * ===================================================== 8. VALIDATE DATA ROWS
			 * =====================================================
			 */
			int totalRows = 0;

			for (int rowIndex = headerRowIndex + 1;

					rowIndex <= sheet.getLastRowNum();

					rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				/*
				 * Ignore completely empty rows.
				 */
				if (row == null || isEmptyRow(row)) {

					continue;
				}

				totalRows++;

				/*
				 * Excel row number.
				 */
				int excelRowNumber = rowIndex + 1;

				/*
				 * ================================================= Validate row.
				 *
				 * Formula cells are evaluated before validation.
				 * =================================================
				 */
				evaluateFormulaCells(row, formulaEvaluator);

				RowValidationError rowError = supplierBillRowValidator.validate(row, excelRowNumber, headerMap);

				if (!rowError.getErrors().isEmpty()) {

					result.getRowErrors().add(rowError);
				}
			}

			/*
			 * ===================================================== 9. TOTAL ROWS
			 * =====================================================
			 */
			result.setTotalRows(totalRows);

			/*
			 * ===================================================== 10. DATA ROW EXISTENCE
			 * =====================================================
			 */
			if (totalRows == 0) {

				result.setValid(false);

				result.getErrors().add("At least one data row is required.");

				return result;
			}

			/*
			 * ===================================================== 11. FINAL RESULT
			 * =====================================================
			 */
			result.setValid(result.getErrors().isEmpty() && result.getRowErrors().isEmpty());

		} catch (Exception e) {

			result.setValid(false);

			result.getErrors().add("Unable to validate file: " + e.getMessage());
		}

		return result;
	}

	/*
	 * ========================================================= MODULE
	 * =========================================================
	 */
	@Override
	protected MigrationType getModuleCode() {
		return MigrationType.SUPPLIER_BILL;
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
	 * Actual Excel:
	 *
	 * Row 6 -> Debit Details / Deduction Details / Net Payable Row 7 -> Actual
	 * column headers
	 *
	 * Therefore Java index = 6
	 * =========================================================
	 */
	@Override
	protected int findHeaderRow(Sheet sheet) {

		if (sheet.getLastRowNum() < HEADER_ROW_INDEX) {
			return -1;
		}

		Row headerRow = sheet.getRow(HEADER_ROW_INDEX);

		if (headerRow == null) {
			return -1;
		}

		/*
		 * Actual headers start from column B.
		 *
		 * B = 1 Y = 24
		 */
		boolean hasSn = false;
		boolean hasUlbName = false;
		boolean hasBillDate = false;
		boolean hasSupplier = false;
		boolean hasPurchaseOrder = false;
		boolean hasDebitGlCode = false;
		boolean hasDeductionGlCode = false;
		boolean hasNetPayableGlCode = false;

		for (Cell cell : headerRow) {

			String value = normalize(cell.toString());

			if (value.equals("sn")) {
				hasSn = true;
			}

			if (value.equals("ulbname")) {
				hasUlbName = true;
			}

			if (value.startsWith("billdate")) {
				hasBillDate = true;
			}

			if (value.equals("supplier")) {
				hasSupplier = true;
			}

			if (value.startsWith("purchaseorder")) {
				hasPurchaseOrder = true;
			}

			if (value.startsWith("glcodeaccountcode")) {

				int column = cell.getColumnIndex();

				/*
				 * First GL Code = Debit Second GL Code = Deduction Third GL Code = Net Payable
				 */
				if (column == 16) {
					hasDebitGlCode = true;
				} else if (column == 19) {
					hasDeductionGlCode = true;
				} else if (column == 23) {
					hasNetPayableGlCode = true;
				}
			}
		}

		if (hasSn && hasUlbName && hasBillDate && hasSupplier && hasPurchaseOrder && hasDebitGlCode
				&& hasDeductionGlCode && hasNetPayableGlCode) {

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

		valid &= validateHeader(headerMap, "supplier", "Supplier", result);

		valid &= validateHeader(headerMap, "purchaseorder", "Purchase Order", result);

		valid &= validateHeader(headerMap, "fund", "Fund", result);

		valid &= validateHeader(headerMap, "department", "Department", result);

		valid &= validateHeader(headerMap, "scheme", "Scheme", result);

		/*
		 * Actual Excel also contains Sub Scheme.
		 */
		valid &= validateHeader(headerMap, "subscheme", "Sub Scheme", result);

		valid &= validateHeader(headerMap, "fundsource", "Fund Source", result);

		valid &= validateHeader(headerMap, "function", "Function", result);

		valid &= validateHeader(headerMap, "narration", "Narration", result);

		valid &= validateHeader(headerMap, "partybillno", "Party Bill No", result);

		valid &= validateHeader(headerMap, "partybilldate", "Party Bill Date", result);

		valid &= validateHeader(headerMap, "billtype", "Bill Type", result);

		/*
		 * ===================================================== DEBIT
		 * =====================================================
		 */
		valid &= validateHeader(headerMap, "debitglcode", "GL Code", result);

		valid &= validateHeader(headerMap, "debitaccounthead", "Account Head", result);

		valid &= validateHeader(headerMap, "debitamount", "Debit Amount", result);

		/*
		 * ===================================================== DEDUCTION
		 * =====================================================
		 */
		valid &= validateHeader(headerMap, "deductionglcode", "GL Code", result);

		valid &= validateHeader(headerMap, "deductionaccounthead", "Account Head", result);

		valid &= validateHeader(headerMap, "deductionpercentage", "Deduction Percentage", result);

		valid &= validateHeader(headerMap, "deductioncreditamount", "Credit Amount", result);

		/*
		 * ===================================================== NET PAYABLE
		 * =====================================================
		 */
		valid &= validateHeader(headerMap, "netpayableglcode", "GL Code", result);

		valid &= validateHeader(headerMap, "netpayableamount", "Credit Amount", result);

		return valid;
	}

	/*
	 * ========================================================= CREATE HEADER MAP
	 *
	 * IMPORTANT:
	 *
	 * Actual Excel columns:
	 *
	 * B = SN C = ULB Name D = Bill Date E = Supplier F = Purchase Order G = Fund H
	 * = Department I = Scheme J = Sub Scheme K = Fund Source L = Function M =
	 * Narration N = Party Bill No O = Party Bill Date P = Bill Type Q = Debit GL
	 * Code R = Debit Account Head S = Debit Amount T = Deduction GL Code U =
	 * Deduction Account Head V = Deduction Percentage W = Deduction Credit Amount X
	 * = Net Payable GL Code Y = Net Payable Credit Amount
	 *
	 * Java indexes:
	 *
	 * B = 1 C = 2 ... Y = 24
	 * =========================================================
	 */
	@Override
	protected Map<String, Integer> createHeaderMap(Row headerRow) {

		Map<String, Integer> headerMap = new HashMap<>();

		if (headerRow == null) {
			return headerMap;
		}

		/*
		 * Use exact column positions because this Excel contains duplicate headers.
		 */
		Map<Integer, String> columnKeys = new HashMap<>();

		columnKeys.put(1, "sn"); // B
		columnKeys.put(2, "ulbname"); // C
		columnKeys.put(3, "billdate"); // D
		columnKeys.put(4, "supplier"); // E
		columnKeys.put(5, "purchaseorder"); // F
		columnKeys.put(6, "fund"); // G
		columnKeys.put(7, "department"); // H
		columnKeys.put(8, "scheme"); // I
		columnKeys.put(9, "subscheme"); // J
		columnKeys.put(10, "fundsource"); // K
		columnKeys.put(11, "function"); // L
		columnKeys.put(12, "narration"); // M
		columnKeys.put(13, "partybillno"); // N
		columnKeys.put(14, "partybilldate"); // O
		columnKeys.put(15, "billtype"); // P

		/*
		 * Debit
		 */
		columnKeys.put(16, "debitglcode"); // Q
		columnKeys.put(17, "debitaccounthead"); // R
		columnKeys.put(18, "debitamount"); // S

		/*
		 * Deduction
		 */
		columnKeys.put(19, "deductionglcode"); // T
		columnKeys.put(20, "deductionaccounthead"); // U
		columnKeys.put(21, "deductionpercentage"); // V
		columnKeys.put(22, "deductioncreditamount"); // W

		/*
		 * Net payable
		 */
		columnKeys.put(23, "netpayableglcode"); // X
		columnKeys.put(24, "netpayableamount"); // Y

		for (Map.Entry<Integer, String> entry : columnKeys.entrySet()) {

			int columnIndex = entry.getKey();
			String key = entry.getValue();

			Cell cell = headerRow.getCell(columnIndex);

			if (cell == null) {
				continue;
			}

			String actualHeader = normalize(cell.toString());

			if (!actualHeader.isEmpty()) {
				headerMap.put(key, columnIndex);
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

	/**
	 * Evaluates formula cells in the given row.
	 *
	 * The evaluated result is stored back into the cell so that DataFormatter can
	 * read the calculated value normally.
	 */
	private void evaluateFormulaCells(Row row, FormulaEvaluator formulaEvaluator) {

		if (row == null) {
			return;
		}

		for (Cell cell : row) {

			if (cell == null) {
				continue;
			}

			if (cell.getCellType() == CellType.FORMULA) {

				formulaEvaluator.evaluateFormulaCell(cell);
			}
		}
	}

}
