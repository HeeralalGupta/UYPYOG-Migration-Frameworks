package org.egov.finance.migration.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.egov.finance.migration.service.validator.JournalVoucherRowValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

	private static final long MAX_FILE_SIZE = 25 * 1024 * 1024;

	/*
	 * We only inspect the first 20 rows while finding the header.
	 */
	private static final int MAX_HEADER_SEARCH_ROWS = 20;

	private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<String>();

	static {
		ALLOWED_EXTENSIONS.add("xls");
		ALLOWED_EXTENSIONS.add("xlsx");
	}

	private final JournalVoucherRowValidator journalVoucherRowValidator;

	public FileValidationService(JournalVoucherRowValidator journalVoucherRowValidator) {
		this.journalVoucherRowValidator = journalVoucherRowValidator;
	}

	public FileValidationResult validate(MultipartFile file, String moduleCode) {

		FileValidationResult result = new FileValidationResult();

		/*
		 * ===================================================== 1. FILE CHECK
		 * =====================================================
		 */

		if (file == null || file.isEmpty()) {

			result.setValid(false);
			result.getErrors().add("Please select an Excel file.");

			return result;
		}

		result.setModuleCode(moduleCode);
		result.setFileName(file.getOriginalFilename());

		/*
		 * ===================================================== 2. FILE SIZE
		 * =====================================================
		 */

		if (file.getSize() > MAX_FILE_SIZE) {
			result.setValid(false);
			result.getErrors().add("File size must not exceed 25 MB.");
			return result;
		}

		/*
		 * ===================================================== 3. FILE EXTENSION
		 * =====================================================
		 */

		String fileName = file.getOriginalFilename();

		if (fileName == null || !hasValidExtension(fileName)) {
			result.setValid(false);
			result.getErrors().add("Only XLS and XLSX files are allowed.");
			return result;
		}

		/*
		 * ===================================================== 4. READ EXCEL
		 * =====================================================
		 */

		try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

			/*
			 * ================================================= 5. CHECK SHEET
			 * =================================================
			 */

			if (workbook.getNumberOfSheets() == 0) {
				result.setValid(false);
				result.getErrors().add("The Excel file does not contain any sheet.");
				return result;
			}

			Sheet sheet = workbook.getSheetAt(0);

			/*
			 * ================================================= 6. FIND MULTI-ROW HEADER
			 * =================================================
			 */

			HeaderInfo headerInfo = findHeaderInfo(sheet);

			if (headerInfo == null) {
				result.setValid(false);
				result.getErrors().add("Unable to identify the Excel header.");
				return result;
			}

			/*
			 * Excel uses 1-based row numbers for users.
			 *
			 * POI uses 0-based row numbers.
			 */

			int headerStartRow = headerInfo.headerStartRow + 1;
			int headerEndRow = headerInfo.headerEndRow + 1;
			int dataStartRow = headerInfo.headerEndRow + 2;

			result.setHeaderStartRow(headerStartRow);
			result.setHeaderEndRow(headerEndRow);
			result.setDataStartRow(dataStartRow);

			/*
			 * ================================================= 
			 * 7. GET LEAF HEADER ROW
			 * =================================================
			 *
			 *
			 * This is the IMPORTANT part.
			 *
			 * For your Journal Voucher:
			 *
			 * Row 3:
			 *
			 * Account Details Subledger Details
			 *
			 * Row 4:
			 *
			 * GLCode DebitAmount CreditAmount LedgerFunctionCode DetailType DetailKey
			 *
			 * Row 4 is therefore the LEAF HEADER.
			 */

			Row leafHeaderRow = sheet.getRow(headerInfo.headerEndRow);

			if (leafHeaderRow == null) {
				result.setValid(false);
				result.getErrors().add("Excel column header row is missing.");
				return result;
			}

			/*
			 * ================================================= 
			 * 8. COUNT COLUMNS
			 * =================================================
			 *
			 * IMPORTANT:
			 *
			 * Do NOT use:
			 *
			 * headers.size()
			 *
			 * because blank cells can exist between columns.
			 *
			 * getLastCellNum() gives the actual column boundary.
			 */

			int columnCount = getActualColumnCount(sheet, headerInfo.headerStartRow, headerInfo.headerEndRow);

			if (columnCount <= 0) {
				result.setValid(false);
				result.getErrors().add("No columns were found in the Excel header.");
				return result;
			}

			result.setColumnCount(columnCount);

			/*
			 * ================================================= 9. READ LEAF HEADERS
			 * =================================================
			 */

			List<String> headers = buildLogicalHeaders(sheet, headerInfo.headerStartRow, headerInfo.headerEndRow,
					columnCount);

			/*
			 * ================================================= 10. DUPLICATE HEADERS
			 * =================================================
			 */

			Set<String> uniqueHeaders = new HashSet<String>();

			for (String header : headers) {
				if (header == null || header.trim().isEmpty()) {
					continue;
				}
				String normalized = normalizeHeader(header);
				if (!uniqueHeaders.add(normalized)) {
					result.getErrors().add("Duplicate column found: " + header);
				}
			}

			/*
			 * ===================================================== 11. COUNT + VALIDATE
			 * DATA ROWS =====================================================
			 */

			int totalRows = 0;

			/*
			 * IMPORTANT:
			 *
			 * dataStartRow is Excel row number.
			 *
			 * POI uses zero-based index.
			 */
			int firstDataRowIndex = headerInfo.headerEndRow + 1;

			Map<String, Integer> headerMap = buildHeaderMap(headers);

			System.out.println("Data Start Row : " + dataStartRow);

			for (int rowIndex = firstDataRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				if (row == null) {
					continue;
				}

				/*
				 * Ignore completely empty rows.
				 */
				if (isEmptyRow(row)) {
					continue;
				}

				totalRows++;

				int excelRowNumber = rowIndex + 1;

				System.out.println("VALIDATING DATA ROW : " + excelRowNumber);

				/*
				 * ================================================= ROW LEVEL VALIDATION
				 * =================================================
				 */

				if ("JOURNAL_VOUCHER".equalsIgnoreCase(moduleCode)) {

					RowValidationError rowError = journalVoucherRowValidator.validate(row, excelRowNumber, headerMap);

					if (!rowError.getErrors().isEmpty()) {

						result.getRowErrors().add(rowError);

						System.out.println("ROW " + excelRowNumber + " FAILED");

						for (String error : rowError.getErrors()) {

							System.out.println("   - " + error);
						}
					}
				}
			}

			result.setTotalRows(totalRows);

			System.out.println("Total Data Rows : " + totalRows);
			System.out.println("Rows With Errors : " + result.getRowErrors().size());

			/*
			 * ================================================= 12. EMPTY DATA CHECK
			 * =================================================
			 */

			if (totalRows == 0) {
				result.getErrors().add("Excel file does not contain any data rows.");
			}

			/*
			 * ================================================= 13. TEMPLATE VALIDATION
			 * =================================================
			 */

			validateAgainstTemplate(moduleCode, headers, result);

		} catch (Exception e) {
			e.printStackTrace();
			result.setErrors(new ArrayList<String>());
			result.getErrors().add(
					"Unable to read the Excel file. " + "Please make sure the file is a valid " + "XLS/XLSX file.");
			result.setValid(false);
			return result;
		}

		/*
		 * ===================================================== FINAL RESULT
		 * =====================================================
		 */

		result.setValid(result.getErrors().isEmpty() && result.getRowErrors().isEmpty());
		return result;
	}

	private List<String> buildLogicalHeaders(Sheet sheet, int headerStartRow, int headerEndRow, int columnCount) {

		List<String> headers = new ArrayList<String>();

		DataFormatter formatter = new DataFormatter();

		for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {

			List<String> parts = new ArrayList<String>();

			/*
			 * Read from bottom header row towards the top header row.
			 */
			for (int rowIndex = headerEndRow; rowIndex >= headerStartRow; rowIndex--) {

				Row row = sheet.getRow(rowIndex);

				if (row == null) {
					continue;
				}

				Cell cell = row.getCell(columnIndex);

				String value = "";

				/*
				 * If this cell belongs to a merged region, get the value from the top-left cell
				 * of that merged region.
				 */
				Cell mergedCell = getMergedCell(sheet, rowIndex, columnIndex);

				if (mergedCell != null) {

					value = formatter.formatCellValue(mergedCell).trim();

				} else if (cell != null) {

					value = formatter.formatCellValue(cell).trim();
				}

				if (!value.isEmpty()) {

					/*
					 * Don't add the same value twice.
					 */
					if (!parts.contains(value)) {

						/*
						 * We are reading bottom-to-top, so add parent at beginning.
						 */
						parts.add(0, value);
					}
				}
			}

			/*
			 * Remove duplicate parent values.
			 */
			List<String> uniqueParts = new ArrayList<String>();

			for (String part : parts) {

				if (!uniqueParts.contains(part)) {
					uniqueParts.add(part);
				}
			}

			/*
			 * Combine multi-level header.
			 *
			 * Example:
			 *
			 * Account Details GLCode *
			 *
			 * becomes:
			 *
			 * Account Details > GLCode *
			 */
			String logicalHeader = String.join(" > ", uniqueParts);

			headers.add(logicalHeader);
		}

		return headers;
	}

	private Cell getMergedCell(Sheet sheet, int rowIndex, int columnIndex) {

		for (CellRangeAddress range : sheet.getMergedRegions()) {

			if (rowIndex >= range.getFirstRow() && rowIndex <= range.getLastRow()
					&& columnIndex >= range.getFirstColumn() && columnIndex <= range.getLastColumn()) {

				Row firstRow = sheet.getRow(range.getFirstRow());

				if (firstRow == null) {
					return null;
				}

				return firstRow.getCell(range.getFirstColumn());
			}
		}

		return null;
	}

	private int getActualColumnCount(Sheet sheet, int headerStartRow, int headerEndRow) {
		int maxColumn = 0;
		DataFormatter formatter = new DataFormatter();
		for (int rowIndex = headerStartRow; rowIndex <= headerEndRow; rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null) {
				continue;
			}
			for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {

				Cell cell = row.getCell(columnIndex);

				if (cell == null) {
					continue;
				}

				String value = formatter.formatCellValue(cell).trim();

				if (!value.isEmpty()) {
					maxColumn = Math.max(maxColumn, columnIndex + 1);
				}
			}
		}

		return maxColumn;
	}

	/*
	 * ========================================================= HEADER INFORMATION
	 * =========================================================
	 */

	private static class HeaderInfo {

		private int headerStartRow;
		private int headerEndRow;

		HeaderInfo(int headerStartRow, int headerEndRow) {

			this.headerStartRow = headerStartRow;
			this.headerEndRow = headerEndRow;
		}
	}

	/*
	 * ========================================================= FIND HEADER
	 * INFORMATION =========================================================
	 *
	 * We look for rows which look like headers.
	 *
	 * A header row normally has:
	 *
	 * 1. Multiple text cells 2. Bold cells 3. Background/fill 4. Merged cells
	 *
	 * This allows us to handle:
	 *
	 * NOTE row TITLE row GROUP HEADER LEAF HEADER DATA
	 *
	 * Example:
	 *
	 * Row 1 -> NOTE Row 2 -> blank Row 3 -> JOURNAL VOUCHER Row 4 -> Account
	 * Details / Subledger Details Row 5 -> GLCode / Debit / Credit ... Row 6 ->
	 * DATA
	 *
	 * Result:
	 *
	 * headerStartRow = Row 4 headerEndRow = Row 5
	 */

	private HeaderInfo findHeaderInfo(Sheet sheet) {

		int lastRow = sheet.getLastRowNum();

		int rowsToCheck = Math.min(lastRow + 1, MAX_HEADER_SEARCH_ROWS);

		List<Integer> candidateRows = new ArrayList<Integer>();

		for (int rowIndex = 0; rowIndex < rowsToCheck; rowIndex++) {

			Row row = sheet.getRow(rowIndex);

			if (row == null) {
				continue;
			}

			int nonEmptyCells = countNonEmptyCells(row);

			if (nonEmptyCells < 2) {

				/*
				 * Ignore:
				 *
				 * NOTE Title single-value rows
				 */

				continue;
			}

			boolean hasBold = hasBoldCells(sheet, row);

			boolean hasFill = hasFilledCells(row);

			boolean hasMerged = hasMergedCells(sheet, rowIndex);

			/*
			 * Header-like row
			 */

			if (hasBold || hasFill || hasMerged) {

				candidateRows.add(rowIndex);
			}
		}

		if (candidateRows.isEmpty()) {

			return null;
		}

		/*
		 * ===================================================== FIND THE LAST
		 * CONTINUOUS HEADER ROW =====================================================
		 *
		 * Example:
		 *
		 * Row 4 -> group header Row 5 -> leaf header
		 *
		 * candidateRows:
		 *
		 * [3, 4]
		 */

		int headerEnd = candidateRows.get(candidateRows.size() - 1);

		/*
		 * Move backwards while rows are continuously header-like.
		 */

		int headerStart = headerEnd;

		for (int i = headerEnd - 1; i >= 0; i--) {

			if (candidateRows.contains(i)) {

				/*
				 * Only accept immediately preceding rows.
				 */

				if (headerStart - i <= 1) {

					headerStart = i;

				} else {

					break;
				}

			} else {

				break;
			}
		}

		return new HeaderInfo(headerStart, headerEnd);
	}

	/*
	 * ========================================================= COUNT NON EMPTY
	 * CELLS =========================================================
	 */

	private int countNonEmptyCells(Row row) {

		int count = 0;

		DataFormatter formatter = new DataFormatter();

		for (Cell cell : row) {

			String value = formatter.formatCellValue(cell).trim();

			if (!value.isEmpty()) {

				count++;
			}
		}

		return count;
	}

	/*
	 * ========================================================= BOLD CELLS
	 * =========================================================
	 */

	private boolean hasBoldCells(Sheet sheet, Row row) {

		int boldCount = 0;
		int nonEmptyCount = 0;

		DataFormatter formatter = new DataFormatter();

		Workbook workbook = sheet.getWorkbook();

		for (Cell cell : row) {

			String value = formatter.formatCellValue(cell).trim();

			if (value.isEmpty()) {
				continue;
			}

			nonEmptyCount++;

			CellStyle style = cell.getCellStyle();

			if (style == null) {
				continue;
			}

			org.apache.poi.ss.usermodel.Font font = workbook.getFontAt(style.getFontIndex());

			if (font != null && font.getBold()) {

				boldCount++;
			}
		}

		return boldCount > 0 && boldCount >= Math.max(1, nonEmptyCount / 2);
	}

	/*
	 * ========================================================= FILLED CELLS
	 * =========================================================
	 */

	private boolean hasFilledCells(Row row) {

		DataFormatter formatter = new DataFormatter();

		int filledCells = 0;

		int nonEmptyCells = 0;

		for (Cell cell : row) {

			String value = formatter.formatCellValue(cell).trim();

			if (value.isEmpty()) {
				continue;
			}

			nonEmptyCells++;

			CellStyle style = cell.getCellStyle();

			if (style != null && style.getFillPattern() != org.apache.poi.ss.usermodel.FillPatternType.NO_FILL) {

				filledCells++;
			}
		}

		return filledCells > 0 && filledCells >= Math.max(1, nonEmptyCells / 2);
	}

	/*
	 * ========================================================= MERGED CELLS
	 * =========================================================
	 */

	private boolean hasMergedCells(Sheet sheet, int rowIndex) {

		for (CellRangeAddress range : sheet.getMergedRegions()) {

			if (rowIndex >= range.getFirstRow() && rowIndex <= range.getLastRow()) {

				return true;
			}
		}

		return false;
	}

	/*
	 * ========================================================= READ LEAF HEADERS
	 * =========================================================
	 *
	 * IMPORTANT:
	 *
	 * We use getLastCellNum() rather than counting only non-empty cells.
	 *
	 * Therefore:
	 *
	 * GLCode DebitAmount CreditAmount ...
	 *
	 * all columns are counted.
	 */

	private List<String> readLeafHeaders(Row headerRow, int columnCount) {

		List<String> headers = new ArrayList<String>();

		DataFormatter formatter = new DataFormatter();

		for (int i = 0; i < columnCount; i++) {

			Cell cell = headerRow.getCell(i);

			if (cell == null) {

				headers.add("");

				continue;
			}

			String value = formatter.formatCellValue(cell).trim();

			headers.add(value);
		}

		return headers;
	}

	/*
	 * ========================================================= EMPTY ROW
	 * =========================================================
	 */

	private boolean isEmptyRow(Row row) {

		DataFormatter formatter = new DataFormatter();

		for (Cell cell : row) {

			String value = formatter.formatCellValue(cell).trim();

			if (!value.isEmpty()) {

				return false;
			}
		}

		return true;
	}

	/*
	 * ========================================================= EXTENSION
	 * =========================================================
	 */

	private boolean hasValidExtension(String fileName) {

		int index = fileName.lastIndexOf('.');

		if (index == -1) {

			return false;
		}

		String extension = fileName.substring(index + 1).toLowerCase();

		return ALLOWED_EXTENSIONS.contains(extension);
	}

	/*
	 * ========================================================= NORMALIZE HEADER
	 * =========================================================
	 */

	private String normalizeHeader(String header) {

		if (header == null) {
			return "";
		}
		return header.trim().toLowerCase().replaceAll("\\s+", "").replace("_", "").replace("-", "");
	}

	/*
	 * ========================================================= TEMPLATE VALIDATION
	 * =========================================================
	 */

	private void validateAgainstTemplate(String moduleCode, List<String> uploadedHeaders, FileValidationResult result) {

		/*
		 * ===================================================== 1. LOAD OFFICIAL
		 * TEMPLATE =====================================================
		 */

		String templatePath = "/migration-templates/" + moduleCode + ".xlsx";

		try (InputStream templateInputStream = getClass().getResourceAsStream(templatePath)) {

			if (templateInputStream == null) {
				result.getWarnings().add("Template not found for module: " + moduleCode);
				return;
			}

			/*
			 * ================================================= 2. READ TEMPLATE
			 * =================================================
			 */

			try (Workbook templateWorkbook = WorkbookFactory.create(templateInputStream)) {

				if (templateWorkbook.getNumberOfSheets() == 0) {
					result.getErrors().add("Template does not contain any sheet.");
					return;
				}

				Sheet templateSheet = templateWorkbook.getSheetAt(0);

				/*
				 * ================================================= 3. FIND TEMPLATE HEADER
				 * =================================================
				 */

				HeaderInfo templateHeaderInfo = findHeaderInfo(templateSheet);

				if (templateHeaderInfo == null) {

					result.getErrors().add("Unable to identify header in " + "module template.");

					return;
				}

				/*
				 * ================================================= 4. FIND TEMPLATE COLUMN
				 * COUNT =================================================
				 */

				int templateColumnCount = getActualColumnCount(templateSheet, templateHeaderInfo.headerStartRow,
						templateHeaderInfo.headerEndRow);

				/*
				 * ================================================= 5. BUILD TEMPLATE LOGICAL
				 * HEADERS =================================================
				 */

				List<String> templateHeaders = buildLogicalHeaders(templateSheet, templateHeaderInfo.headerStartRow,
						templateHeaderInfo.headerEndRow, templateColumnCount);

				/*
				 * ================================================= 8. COMPARE
				 * =================================================
				 */

				compareHeaders(templateHeaders, uploadedHeaders, result);
			}

		} catch (Exception e) {

			e.printStackTrace();

			result.getErrors().add("Unable to validate Excel headers " + "against the module template.");
		}
	}

	private void compareHeaders(List<String> expectedHeaders, List<String> uploadedHeaders,
			FileValidationResult result) {

		/*
		 * ===================================================== NORMALIZED LISTS
		 * =====================================================
		 */

		List<String> expectedNormalized = new ArrayList<String>();
		List<String> uploadedNormalized = new ArrayList<String>();

		for (String header : expectedHeaders) {
			expectedNormalized.add(normalizeHeader(header));
		}

		for (String header : uploadedHeaders) {

			uploadedNormalized.add(normalizeHeader(header));
		}

		/*
		 * ===================================================== 1. COLUMN COUNT
		 * =====================================================
		 */

		if (expectedHeaders.size() != uploadedHeaders.size()) {

			result.getErrors().add("Column count mismatch. " + "Expected " + expectedHeaders.size()
					+ " columns but found " + uploadedHeaders.size() + ".");
		}

		/*
		 * ===================================================== 2. MISSING COLUMNS
		 * =====================================================
		 */

		for (int i = 0; i < expectedNormalized.size(); i++) {

			String expected = expectedNormalized.get(i);
			if (!uploadedNormalized.contains(expected)) {

				result.getErrors().add("Missing column: " + expectedHeaders.get(i));
			}
		}

		/*
		 * ===================================================== 3. EXTRA COLUMNS
		 * =====================================================
		 */

		for (int i = 0; i < uploadedNormalized.size(); i++) {

			String uploaded = uploadedNormalized.get(i);

			if (!expectedNormalized.contains(uploaded)) {
				result.getErrors().add("Unexpected column: " + uploadedHeaders.get(i));
			}
		}

		/*
		 * ===================================================== 4. COLUMN ORDER
		 * =====================================================
		 */

		int compareCount = Math.min(expectedNormalized.size(), uploadedNormalized.size());

		for (int i = 0; i < compareCount; i++) {

			String expected = expectedNormalized.get(i);
			String uploaded = uploadedNormalized.get(i);
			if (!expected.equals(uploaded)) {

				result.getErrors().add("Column order mismatch at " + "position " + (i + 1) + ". Expected: "
						+ expectedHeaders.get(i) + " but found: " + uploadedHeaders.get(i));
			}
		}

		/*
		 * ===================================================== 5. DUPLICATE COLUMNS
		 * =====================================================
		 */

		Set<String> uniqueUploaded = new HashSet<String>();

		for (String header : uploadedNormalized) {

			if (!uniqueUploaded.add(header)) {

				/*
				 * Don't report empty values as duplicates.
				 */
				if (!header.isEmpty()) {

					result.getErrors().add("Duplicate column: " + header);
				}
			}
		}

	}

	private Map<String, Integer> buildHeaderMap(List<String> headers) {

		Map<String, Integer> headerMap = new java.util.HashMap<String, Integer>();

		for (int i = 0; i < headers.size(); i++) {

			String header = normalizeHeader(headers.get(i));

			/*
			 * Logical headers may contain:
			 *
			 * Account Details > GLCode *
			 *
			 * We only need the leaf name here.
			 */

			if (header.contains(">")) {

				String[] parts = header.split(">");

				header = parts[parts.length - 1];
			}

			header = header.replace("*", "").trim();

			headerMap.put(normalizeHeader(header), i);
		}

		return headerMap;
	}
}