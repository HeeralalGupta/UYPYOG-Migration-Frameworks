
package org.egov.finance.migration.modules.banktobank.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferRecord;
import org.springframework.stereotype.Service;

@Service
public class BankToBankTransferExcelReader {

	private final DataFormatter formatter = new DataFormatter();


	public List<BankToBankTransferRecord> read(String filePath) {

		List<BankToBankTransferRecord> records = new ArrayList<>();

		try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
				Workbook workbook = WorkbookFactory.create(inputStream)) {

			Sheet sheet = null;

			// Find required sheet.
			for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

				if (ExcelConstants.BANK_TO_BANK_TRANSFER_SHEET .equalsIgnoreCase(workbook.getSheetName(i))) {

					sheet = workbook.getSheetAt(i);
					break;
				}
			}

			if (sheet == null) {
				throw new IllegalArgumentException("Excel sheet '" + ExcelConstants.BANK_TO_BANK_TRANSFER_SHEET  + "' not found.");
			}

			// Find header row dynamically.
			int headerRowIndex = findHeaderRow(sheet);

			// Build header mapping.
			Map<String, Integer> headerMap = buildHeaderMap(sheet.getRow(headerRowIndex));

			// Read records after header row.
			for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				if (row == null || isEmptyRow(row)) {
					continue;
				}

				BankToBankTransferRecord record = createRecord(row, headerMap);

				// Actual Excel row number.
				record.setRowNumber(rowIndex + 1);

				records.add(record);
			}

		} catch (Exception e) {

			throw new RuntimeException("Unable to read Bank To Bank Transfer Excel file.", e);
		}

		return records;
	}

	/**
	 * Find header row dynamically.
	 */
	private int findHeaderRow(Sheet sheet) {

		for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

			Row row = sheet.getRow(rowIndex);

			if (row == null) {
				continue;
			}

			int matchedHeaders = 0;

			for (Cell cell : row) {

				String header = normalizeHeader(formatter.formatCellValue(cell));

				if (isBankTransferHeader(header)) {
					matchedHeaders++;
				}
			}

			// At least 10 known headers required.
			if (matchedHeaders >= 10) {
				return rowIndex;
			}
		}

		throw new IllegalArgumentException("Bank To Bank Transfer Excel header row not found.");
	}

	/**
	 * Check known Bank To Bank Transfer headers.
	 */
	private boolean isBankTransferHeader(String header) {

		return "serialnumber".equals(header) || "ulbname".equals(header) || "voucherdate".equals(header)
				|| "fromfund".equals(header) || "fromsubscheme".equals(header) || "fromdepartment".equals(header)
				|| "function".equals(header) || "frombank".equals(header) || "fromaccountno".equals(header)
				|| "tofund".equals(header) || "todepartment".equals(header) || "tobank".equals(header)
				|| "toaccountno".equals(header) || "sourceinterfundcode".equals(header)
				|| "destinationinterfundcode".equals(header) || "modeoftransaction".equals(header)
				|| "referencenumber".equals(header) || "referencedate".equals(header) || "amountrs".equals(header)
				|| "narration".equals(header);
	}

	/**
	 * Build header name -> column index mapping.
	 */
	private Map<String, Integer> buildHeaderMap(Row headerRow) {

		Map<String, Integer> headerMap = new HashMap<>();

		for (Cell cell : headerRow) {

			String header = normalizeHeader(formatter.formatCellValue(cell));

			if (!header.isEmpty()) {

				headerMap.put(header, cell.getColumnIndex());
			}
		}

		return headerMap;
	}

	/**
	 * Normalize Excel header.
	 *
	 * Example: Voucher Date* -> voucherdate Amount (Rs.)* -> amountrs
	 */
	private String normalizeHeader(String value) {

		if (value == null) {
			return "";
		}

		return value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	/**
	 * Convert Excel row into BankToBankTransferRecord.
	 */
	private BankToBankTransferRecord createRecord(Row row, Map<String, Integer> headerMap) {

		BankToBankTransferRecord record = new BankToBankTransferRecord();

		record.setSerialNumber(parseInteger(getCellValue(row, headerMap, "serialnumber")));

		record.setUlbName(getCellValue(row, headerMap, "ulbname"));

		record.setVoucherDate(parseDate(getCell(row, headerMap, "voucherdate")));

		record.setFromFund(getCellValue(row, headerMap, "fromfund"));

		record.setFromSubScheme(getCellValue(row, headerMap, "fromsubscheme"));

		record.setFromDepartment(getCellValue(row, headerMap, "fromdepartment"));

		record.setFunction(getCellValue(row, headerMap, "function"));

		record.setFromBank(getCellValue(row, headerMap, "frombank"));

		record.setFromAccountNumber(getCellValue(row, headerMap, "fromaccountno"));

		record.setToFund(getCellValue(row, headerMap, "tofund"));

		record.setToDepartment(getCellValue(row, headerMap, "todepartment"));

		record.setToBank(getCellValue(row, headerMap, "tobank"));

		record.setToAccountNumber(getCellValue(row, headerMap, "toaccountno"));

		record.setSourceInterFundCode(getCellValue(row, headerMap, "sourceinterfundcode"));

		record.setDestinationInterFundCode(getCellValue(row, headerMap, "destinationinterfundcode"));

		record.setModeOfTransaction(getCellValue(row, headerMap, "modeoftransaction"));

		record.setReferenceNumber(getCellValue(row, headerMap, "referencenumber"));

		record.setReferenceDate(parseDate(getCell(row, headerMap, "referencedate")));

		record.setAmount(parseBigDecimal(getCellValue(row, headerMap, "amountrs")));

		record.setNarration(getCellValue(row, headerMap, "narration"));

		return record;
	}

	/**
	 * Get cell using dynamic header mapping.
	 */
	private Cell getCell(Row row, Map<String, Integer> headerMap, String header) {

		Integer columnIndex = headerMap.get(header);

		if (columnIndex == null) {
			return null;
		}

		return row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
	}

	/**
	 * Get cell value using dynamic header mapping.
	 */
	private String getCellValue(Row row, Map<String, Integer> headerMap, String header) {

		Cell cell = getCell(row, headerMap, header);

		if (cell == null) {
			return "";
		}

		return formatter.formatCellValue(cell).trim();
	}

	/**
	 * Parse integer value.
	 */
	private Integer parseInteger(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			return new BigDecimal(value.trim()).intValueExact();

		} catch (ArithmeticException | NumberFormatException e) {

			throw new IllegalArgumentException("Invalid serial number: " + value);
		}
	}

	/**
	 * Parse numeric value into BigDecimal.
	 */
	private BigDecimal parseBigDecimal(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			return new BigDecimal(value.replace(",", "").trim());

		} catch (NumberFormatException e) {

			throw new IllegalArgumentException("Invalid transfer amount: " + value);
		}
	}

	/**
	 * Parse Excel date.
	 */
	private Date parseDate(Cell cell) {

		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}

		// Excel native date cell.
		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {

			return cell.getDateCellValue();
		}

		// Formula cell returning date.
		if (cell.getCellType() == CellType.FORMULA && DateUtil.isCellDateFormatted(cell)) {

			return cell.getDateCellValue();
		}

		// Date stored as text.
		String value = formatter.formatCellValue(cell).trim();

		if (value.isEmpty()) {
			return null;
		}

		String[] formats = { "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "yyyy-MM-dd", "yyyy/MM/dd", "yyyy.MM.dd",
				"MM/dd/yyyy", "MM-dd-yyyy", "dd/MM/yyyy HH:mm:ss", "dd-MM-yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss" };

		for (String format : formats) {

			try {

				SimpleDateFormat dateFormat = new SimpleDateFormat(format);

				dateFormat.setLenient(false);

				return dateFormat.parse(value);

			} catch (ParseException ignored) {
				// Try next format.
			}
		}

		throw new IllegalArgumentException("Invalid date value: " + value);
	}

	/**
	 * Check whether complete row is empty.
	 */
	private boolean isEmptyRow(Row row) {

		for (Cell cell : row) {

			if (!formatter.formatCellValue(cell).trim().isEmpty()) {

				return false;
			}
		}

		return true;
	}
}
