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
import org.egov.finance.migration.service.validator.ExpenseBillRowValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExpenseBillFileValidationService extends AbstractFileValidationService {

	/**
	 * Actual Expense Bill header row:
	 *
	 * Excel Row 4 = Java index 3
	 */
	private static final int HEADER_ROW_INDEX = 3;

	private final ExpenseBillRowValidator expenseBillRowValidator;

	public ExpenseBillFileValidationService(ExpenseBillRowValidator expenseBillRowValidator) {

		super(expenseBillRowValidator);
		this.expenseBillRowValidator = expenseBillRowValidator;
	}

	/**
	 * ========================================================= VALIDATE FILE
	 * =========================================================
	 */
	@Override
	public FileValidationResult validate(MultipartFile file) {

		FileValidationResult result = new FileValidationResult();

		result.setFileName(file.getOriginalFilename());
		result.setModuleCode(getModuleCode().toString());

		try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

			/*
			 * ===================================================== 1. FORMULA EVALUATOR
			 * =====================================================
			 *
			 * Allows validation of cells containing formulas.
			 */
			FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

			/*
			 * ===================================================== 2. GET EXPENSE BILL
			 * SHEET =====================================================
			 */
			Sheet sheet = getSheet(workbook);

			if (sheet == null) {

				result.setValid(false);
				result.getErrors().add("Expense Bill sheet not found.");
				return result;
			}

			/*
			 * ===================================================== 3. FIND HEADER ROW
			 * =====================================================
			 *
			 * Expense Bill:
			 *
			 * Excel Row 3 -> Grouped headers Excel Row 4 -> Actual column headers
			 *
			 * Java index = 3
			 */
			int headerRowIndex = findHeaderRow(sheet);

			if (headerRowIndex == -1) {
				result.setValid(false);
				result.getErrors().add("Header row & column not found in the Excel sheet. "
								+ "Please update the Excel sheet with proper headers "
								+ "and mandatory columns and Try again.");

				return result;
			}

			/*
			 * ===================================================== 4. HEADER INFORMATION
			 * =====================================================
			 */
			result.setHeaderRow(headerRowIndex + 1);
			result.setHeaderStartRow(headerRowIndex + 1);
			result.setHeaderEndRow(headerRowIndex + 1);

			/*
			 * ===================================================== 5. CREATE HEADER MAP
			 * =====================================================
			 *
			 * Expense Bill contains duplicate headers:
			 *
			 * GL Code Account Head Credit Amount
			 *
			 * Therefore exact column positions are used.
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
			 * =====================================================
			 *
			 * Header = Excel Row 4 Data = Excel Row 5
			 */
			result.setDataStartRow(headerRowIndex + 2);

			/*
			 * ===================================================== 8. VALIDATE DATA ROWS
			 * =====================================================
			 */
			int totalRows = 0;

			for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				/*
				 * Ignore completely empty rows.
				 */
				if (row == null || isEmptyRow(row)) {
					continue;
				}

				totalRows++;

				/*
				 * Excel row number starts from 1.
				 */
				int excelRowNumber = rowIndex + 1;

				/*
				 * ================================================= Evaluate formula cells
				 * before validation. =================================================
				 */
				evaluateFormulaCells(row, formulaEvaluator);

				/*
				 * ================================================= Validate row.
				 * =================================================
				 */
				RowValidationError rowError = expenseBillRowValidator.validate(row, excelRowNumber, headerMap);

				if (rowError != null && !rowError.getErrors().isEmpty()) {

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
		return MigrationType.EXPENSE_BILL;
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

		return workbook.getSheet("Expense Bill");
	}

	/*
	 * ========================================================= FIND HEADER ROW
	 * =========================================================
	 *
	 * Actual Expense Bill Excel:
	 *
	 * Row 3 -> Grouped headers Row 4 -> Actual column headers
	 *
	 * Therefore Java index = 3
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
		 * ===================================================== Required headers
		 * =====================================================
		 */

		boolean hasSn = false;
		boolean hasUlbName = false;
		boolean hasBillDate = false;
		boolean hasPartyBillDate = false;
		boolean hasPartyBillNo = false;
		boolean hasFund = false;
		boolean hasDepartment = false;
		boolean hasScheme = false;
		boolean hasFundSource = false;
		boolean hasFunction = false;
		boolean hasNarration = false;
		boolean hasBillSubType = false;
		boolean hasCategoryType = false;
		boolean hasSubledgerMaster = false;
		boolean hasDebitGlCode = false;
		boolean hasDeductionGlCode = false;
		boolean hasNetPayableGlCode = false;
		boolean hasDebitAccountHead = false;
		boolean hasDebitAmount = false;
		boolean hasDeductionAccountHead = false;
		boolean hasDeductionCreditAmount = false;
		boolean hasNetPaybleCreditAmount = false;

		for (Cell cell : headerRow) {

			String value = normalize(cell.toString());
			int column = cell.getColumnIndex();

			/*
			 * ================================================= BASIC COLUMNS
			 * =================================================
			 */

			if (column == 0 && value.equals("sn")) {
				hasSn = true;
			}

			if (column == 1 && value.equals("ulbname")) {
				hasUlbName = true;
			}

			if (column == 2 && value.startsWith("billdate")) {
				hasBillDate = true;
			}

			if (column == 3 && value.equals("fund")) {
				hasFund = true;
			}

			if (column == 4 && value.equals("department")) {
				hasDepartment = true;
			}

			if (column == 5 && value.equals("scheme")) {
				hasScheme = true;
			}

			if (column == 6 && value.equals("fundsource")) {
				hasFundSource = true;
			}

			if (column == 7 && value.equals("function")) {
				hasFunction = true;
			}

			if (column == 8 && value.equals("narration")) {
				hasNarration = true;
			}
			if (column == 9 && value.equals("partybillno")) {
				hasPartyBillNo = true;
			}
			if (column == 10 && value.startsWith("partybilldate")) {
				hasPartyBillDate = true;
			}
			if (column == 11 && value.startsWith("billsubtype")) {
				hasBillSubType = true;
			}
			if (column == 12 && value.startsWith("categorytype")) {
				hasCategoryType = true;
			}
			if (column == 13 && value.equals("subledgermaster")) {
				hasSubledgerMaster = true;
			}
			if (column == 21 && value.equals("gicodeaccountcode")) {
				hasNetPayableGlCode = true;
			}
			if (value.equals("accounthead")) {
				if(column == 15 ) {
					hasDebitAccountHead = true;
				}
				if(column == 18 ) {
					hasDeductionAccountHead = true;
				}
				
			}
			if (column == 16 && value.equals("debitamount")) {
				hasDebitAmount = true;
			}
			if (value.equals("creditamount")) {
				if(column == 20 ) {
					hasDeductionCreditAmount = true;
				}
				if(column == 22 ) {
					hasNetPaybleCreditAmount = true;
				}
			}
			

			/*
			 * ================================================= GL CODE COLUMNS
			 * =================================================
			 *
			 * Expense Bill has duplicate "GL Code".
			 *
			 * O = Debit GL Code = 14 R = Deduction GL Code = 17 V = Net Payable GL Code = 21
			 */
			if (value.startsWith("glcodeaccountcode")) {
				/*
				 * First GL Code = Debit Second GL Code = Deduction Third GL Code = Net Payable
				 */
				if (column == 14) {
					hasDebitGlCode = true;
				} else if (column == 17) {
					hasDeductionGlCode = true;
				}
			}
		}

		/*
		 * ===================================================== HEADER FOUND
		 * =====================================================
		 */
		if (hasSn && hasUlbName && hasBillDate && hasPartyBillNo && hasPartyBillDate && hasFund && hasDepartment && hasScheme && hasFundSource && hasFunction
				&& hasNarration && hasBillSubType && hasCategoryType && hasSubledgerMaster && hasDebitGlCode && hasDebitAccountHead && hasDebitAmount && hasDeductionGlCode && hasDeductionAccountHead &&hasDeductionCreditAmount && hasNetPayableGlCode && hasNetPaybleCreditAmount) {

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

		/*
		 * ===================================================== BASIC DETAILS
		 * =====================================================
		 */
		valid &= validateHeader(headerMap, "sn", "SN.", result);

		valid &= validateHeader(headerMap, "ulbname", "ULB Name", result);

		valid &= validateHeader(headerMap, "billdate", "Bill Date", result);

		valid &= validateHeader(headerMap, "fund", "Fund", result);

		valid &= validateHeader(headerMap, "department", "Department", result);

		valid &= validateHeader(headerMap, "scheme", "Scheme", result);

		valid &= validateHeader(headerMap, "fundsource", "Fund Source", result);

		valid &= validateHeader(headerMap, "function", "Function", result);

		valid &= validateHeader(headerMap, "narration", "Narration", result);

		/*
		 * ===================================================== PARTY BILL DETAILS
		 * =====================================================
		 */
		valid &= validateHeader(headerMap, "partybillno", "Party Bill No", result);

		valid &= validateHeader(headerMap, "partybilldate", "Party Bill Date", result);

		/*
		 * ===================================================== BILL DETAILS
		 * =====================================================
		 */
		valid &= validateHeader(headerMap, "billsubtype", "Bill SubType", result);

		valid &= validateHeader(headerMap, "subledgertype", "SubLedger Type", result);

		valid &= validateHeader(headerMap, "subledgermaster", "SubLedger Master", result);

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
	 * =========================================================
	 *
	 * Actual Expense Bill columns:
	 *
	 * A = SN B = ULB Name C = Bill Date D = Fund E = Department F = Scheme G = Fund
	 * Source H = Function I = Narration J = Party Bill No K = Party Bill Date L =
	 * Bill SubType M = SubLedger Type N = SubLedger Master O = Debit GL Code P =
	 * Debit Account Head Q = Debit Amount R = Deduction GL Code S = Deduction
	 * Account Head T = Deduction Percentage U = Deduction Credit Amount V = Net
	 * Payable GL Code W = Net Payable Credit Amount
	 *
	 * Java indexes:
	 *
	 * A = 0 ... W = 22 =========================================================
	 */
	@Override
	protected Map<String, Integer> createHeaderMap(Row headerRow) {

		Map<String, Integer> headerMap = new HashMap<>();

		if (headerRow == null) {
			return headerMap;
		}

		/*
		 * Use exact column positions because Expense Bill contains duplicate logical
		 * headers.
		 */
		Map<Integer, String> columnKeys = new HashMap<>();

		/*
		 * ===================================================== BASIC DETAILS
		 * =====================================================
		 */
		columnKeys.put(0, "sn"); // A
		columnKeys.put(1, "ulbname"); // B
		columnKeys.put(2, "billdate"); // C
		columnKeys.put(3, "fund"); // D
		columnKeys.put(4, "department"); // E
		columnKeys.put(5, "scheme"); // F
		columnKeys.put(6, "fundsource"); // G
		columnKeys.put(7, "function"); // H
		columnKeys.put(8, "narration"); // I

		/*
		 * ===================================================== PARTY BILL
		 * =====================================================
		 */
		columnKeys.put(9, "partybillno"); // J
		columnKeys.put(10, "partybilldate"); // K

		/*
		 * ===================================================== BILL DETAILS
		 * =====================================================
		 */
		columnKeys.put(11, "billsubtype"); // L
		columnKeys.put(12, "subledgertype"); // M
		columnKeys.put(13, "subledgermaster"); // N

		/*
		 * ===================================================== DEBIT
		 * =====================================================
		 */
		columnKeys.put(14, "debitglcode"); // O
		columnKeys.put(15, "debitaccounthead"); // P
		columnKeys.put(16, "debitamount"); // Q

		/*
		 * ===================================================== DEDUCTION
		 * =====================================================
		 */
		columnKeys.put(17, "deductionglcode"); // R
		columnKeys.put(18, "deductionaccounthead"); // S
		columnKeys.put(19, "deductionpercentage"); // T
		columnKeys.put(20, "deductioncreditamount"); // U

		/*
		 * ===================================================== NET PAYABLE
		 * =====================================================
		 */
		columnKeys.put(21, "netpayableglcode"); // V
		columnKeys.put(22, "netpayableamount"); // W

		/*
		 * ===================================================== BUILD HEADER MAP
		 * =====================================================
		 */
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

	/*
	 * ========================================================= EVALUATE FORMULA
	 * CELLS =========================================================
	 *
	 * Evaluates formula cells before row validation.
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