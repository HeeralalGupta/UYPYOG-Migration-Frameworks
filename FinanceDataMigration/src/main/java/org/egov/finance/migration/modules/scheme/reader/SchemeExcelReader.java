package org.egov.finance.migration.modules.scheme.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.scheme.dto.SchemeRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SchemeExcelReader {

	/*
	 * =========================================================
	 * EXCEL ROW CONFIGURATION
	 * =========================================================
	 *
	 * Row 1 -> Scheme Master
	 * Row 2 -> Note
	 * Row 3 -> Header
	 * Row 4 -> First data row
	 */

	private static final int HEADER_ROW_1 = 2;
	private static final int HEADER_ROW_2 = 3;

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
	 * Column C = Scheme Name
	 * Column D = Fund
	 * Column E = Status
	 * Column F = Start Date
	 * Column G = End Date
	 * Column H = Description
	 *
	 * Java index:
	 *
	 * 0, 1, 2, 3, 4, 5, 6, 7
	 */

	private static final int COL_SERIAL_NUMBER = 0;
	private static final int COL_ULB_NAME = 1;
	private static final int COL_SCHEME_NAME = 2;
	private static final int COL_FUND = 3;
	private static final int COL_STATUS = 4;
	private static final int COL_START_DATE = 5;
	private static final int COL_END_DATE = 6;
	private static final int COL_DESCRIPTION = 7;

	private final DataFormatter formatter = new DataFormatter();

	/**
	 * Read Scheme Master Excel file.
	 */
	public List<SchemeRecord> read(MultipartFile file) {

		List<SchemeRecord> records = new ArrayList<>();

		try (InputStream inputStream = file.getInputStream();
				Workbook workbook = WorkbookFactory.create(inputStream)) {

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
				 * Create Scheme record.
				 */
				SchemeRecord record = createSchemeRecord(row);

				record.setStartRow(excelRowNumber);
				record.setEndRow(excelRowNumber);

				records.add(record);
			}

		} catch (Exception e) {

			throw new RuntimeException("Unable to read Scheme Master Excel file.", e);
		}

		return records;
	}

	/*
	 * =========================================================
	 * CREATE SCHEME RECORD
	 * =========================================================
	 */
	private SchemeRecord createSchemeRecord(Row row) {

		SchemeRecord record = new SchemeRecord();

		/*
		 * Sl. No.
		 */
		record.setSerialNumber(
				parseInteger(getCellValue(row, COL_SERIAL_NUMBER)));

		/*
		 * ULB Name
		 */
		record.setUlbName(
				getCellValue(row, COL_ULB_NAME));

		/*
		 * Scheme Name
		 */
		record.setSchemeName(
				getCellValue(row, COL_SCHEME_NAME));

		/*
		 * Fund
		 */
		record.setFundName(
				getCellValue(row, COL_FUND));

		/*
		 * Status
		 */
		record.setIsActive(
				getCellValue(row, COL_STATUS));

		/*
		 * Start Date
		 */
		record.setValidFrom(
				getCellValue(row, COL_START_DATE));

		/*
		 * End Date
		 */
		record.setValidTo(
				getCellValue(row, COL_END_DATE));

		/*
		 * Description
		 */
		record.setDescription(
				getCellValue(row, COL_DESCRIPTION));

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
			 * Handles values such as:
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
		 * Only check the columns used by Scheme Master.
		 *
		 * A -> Sl. No.
		 * B -> ULB Name
		 * C -> Scheme Name
		 * D -> Fund
		 * E -> Status
		 * F -> Start Date
		 * G -> End Date
		 * H -> Description
		 */

		for (int i = COL_SERIAL_NUMBER; i <= COL_DESCRIPTION; i++) {

			if (!getCellValue(row, i).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}