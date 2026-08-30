package org.egov.finance.migration.modules.bank.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.bank.dto.BankRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BankExcelReader {

	/*
	 * =========================================================
	 * EXCEL ROW CONFIGURATION
	 * =========================================================
	 *
	 * Row 1 -> Bank Master
	 * Row 2 -> Note Row
	 * Row 3 -> Header Row
	 * Row 4 -> First data row
	 */

	private static final int HEADER_ROW = 3;
	private static final int DATA_START_ROW = 4;

	/*
	 * =========================================================
	 * EXCEL COLUMN CONFIGURATION
	 * =========================================================
	 *
	 * Excel:
	 *
	 * Column A = Sl. No.
	 * Column B = ULB Name
	 * Column C = Bank Name
	 * Column D = Narration
	 *
	 * Java index:
	 *
	 * A -> 0
	 * B -> 1
	 * C -> 2
	 * D -> 3
	 */

	private static final int COL_SERIAL_NUMBER = 0;
	private static final int COL_ULB_NAME = 1;
	private static final int COL_BANK_NAME = 2;
	private static final int COL_NARRATION = 3;

	private final DataFormatter formatter = new DataFormatter();

	/**
	 * Read Bank Master Excel file.
	 */
	public List<BankRecord> read(MultipartFile file) {

		List<BankRecord> records = new ArrayList<>();

		try (InputStream inputStream = file.getInputStream();
				Workbook workbook = WorkbookFactory.create(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);

			for (int rowIndex = DATA_START_ROW - 1;
					rowIndex <= sheet.getLastRowNum();
					rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				/*
				 * Skip null / empty rows.
				 */
				if (row == null || isEmptyRow(row)) {
					continue;
				}

				/*
				 * POI row index starts from 0.
				 *
				 * Excel row number starts from 1.
				 */
				int excelRowNumber = rowIndex + 1;

				/*
				 * Create Bank record.
				 */
				BankRecord record = createBankRecord(row);

				record.setStartRow(excelRowNumber);
				record.setEndRow(excelRowNumber);

				records.add(record);
			}

		} catch (Exception e) {

			throw new RuntimeException(
					"Unable to read Bank Master Excel file.", e);
		}

		return records;
	}

	/*
	 * =========================================================
	 * CREATE BANK RECORD
	 * =========================================================
	 */
	private BankRecord createBankRecord(Row row) {

		BankRecord record = new BankRecord();

		/*
		 * Sl. No.
		 */
		record.setSerialNumber(
				parseInteger(
						getCellValue(row, COL_SERIAL_NUMBER)));

		/*
		 * ULB Name
		 */
		record.setUlbName(
				getCellValue(row, COL_ULB_NAME));

		/*
		 * Bank Name
		 */
		record.setBankName(
				getCellValue(row, COL_BANK_NAME));

		/*
		 * Narration
		 */
		record.setNarration(
				getCellValue(row, COL_NARRATION));

		return record;
	}

	/*
	 * =========================================================
	 * CELL VALUE
	 * =========================================================
	 */
	private String getCellValue(Row row, int columnIndex) {

		Cell cell = row.getCell(
				columnIndex,
				Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return "";
		}

		return formatter.formatCellValue(cell).trim();
	}

	/*
	 * =========================================================
	 * INTEGER
	 * =========================================================
	 */
	private Integer parseInteger(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {

			/*
			 * Handles:
			 *
			 * 1
			 * 2
			 * 3
			 * 1.0
			 */

			return (int) Double.parseDouble(
					value.replace(",", "").trim());

		} catch (NumberFormatException e) {

			return null;
		}
	}

	/*
	 * =========================================================
	 * EMPTY ROW
	 * =========================================================
	 */
	private boolean isEmptyRow(Row row) {

		/*
		 * Check all columns used by Bank Master.
		 *
		 * A -> Sl. No.
		 * B -> ULB Name
		 * C -> Bank Name
		 * D -> Narration
		 */

		for (int i = COL_SERIAL_NUMBER;
				i <= COL_NARRATION;
				i++) {

			if (!getCellValue(row, i).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}