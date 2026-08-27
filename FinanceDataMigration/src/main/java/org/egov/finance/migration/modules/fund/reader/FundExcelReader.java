package org.egov.finance.migration.modules.fund.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.fund.dto.FundRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FundExcelReader {

	/*
	 * ========================================================= EXCEL ROW
	 * CONFIGURATION =========================================================
	 *
	 * Row 1 -> Fund Master Row 2 -> Note Row 3 -> Header Row 4 -> First data row
	 */

	private static final int HEADER_ROW_1 = 2;
	private static final int HEADER_ROW_2 = 3;

	private static final int DATA_START_ROW = 4;

	/*
	 * ========================================================= EXCEL COLUMN
	 * CONFIGURATION =========================================================
	 *
	 * Excel:
	 *
	 * Column A = Sl. No. Column B = ULB Name Column C = Fund Name Column D = Nature
	 * of Fund Column E = Parent Fund
	 *
	 * Java index:
	 *
	 * 0, 1, 2, 3, 4
	 */

	private static final int COL_SERIAL_NUMBER = 0;
	private static final int COL_ULB_NAME = 1;
	private static final int COL_FUND_NAME = 2;
	private static final int COL_NATURE_OF_FUND = 3;
	private static final int COL_PARENT_FUND = 4;

	private final DataFormatter formatter = new DataFormatter();

	/**
	 * Read Fund Master Excel file.
	 */
	public List<FundRecord> read(MultipartFile file) {

		List<FundRecord> records = new ArrayList<>();

		try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);

			for (int rowIndex = DATA_START_ROW - 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

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
				 * Create Fund record.
				 */
				FundRecord record = createFundRecord(row);

				record.setStartRow(excelRowNumber);
				record.setEndRow(excelRowNumber);

				records.add(record);
			}

		} catch (Exception e) {

			throw new RuntimeException("Unable to read Fund Master Excel file.", e);
		}

		return records;
	}

	/*
	 * ========================================================= CREATE FUND RECORD
	 * =========================================================
	 */
	private FundRecord createFundRecord(Row row) {

		FundRecord record = new FundRecord();

		/*
		 * Sl. No.
		 */
		record.setSerialNumber(parseInteger(getCellValue(row, COL_SERIAL_NUMBER)));

		/*
		 * ULB Name
		 */
		record.setUlbName(getCellValue(row, COL_ULB_NAME));

		/*
		 * Fund Name
		 */
		record.setFundName(getCellValue(row, COL_FUND_NAME));

		/*
		 * Nature of Fund
		 */
		record.setNatureOfFund(getCellValue(row, COL_NATURE_OF_FUND));

		/*
		 * Parent Fund
		 */
		record.setParentFund(getCellValue(row, COL_PARENT_FUND));

		return record;
	}

	/*
	 * ========================================================= CELL VALUE
	 * =========================================================
	 */
	private String getCellValue(Row row, int columnIndex) {

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return "";
		}

		return formatter.formatCellValue(cell).trim();
	}

	/*
	 * ========================================================= INTEGER
	 * =========================================================
	 */
	private Integer parseInteger(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {

			/*
			 * Handles values such as:
			 *
			 * 1 2 3 1.0
			 */

			return (int) Double.parseDouble(value.replace(",", "").trim());

		} catch (NumberFormatException e) {

			return null;
		}
	}

	/*
	 * ========================================================= EMPTY ROW
	 * =========================================================
	 */
	private boolean isEmptyRow(Row row) {

		/*
		 * Only check the columns used by Fund Master.
		 *
		 * A -> Sl. No. B -> ULB Name C -> Fund Name D -> Nature of Fund E -> Parent
		 * Fund
		 */

		for (int i = COL_SERIAL_NUMBER; i <= COL_PARENT_FUND; i++) {

			if (!getCellValue(row, i).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}