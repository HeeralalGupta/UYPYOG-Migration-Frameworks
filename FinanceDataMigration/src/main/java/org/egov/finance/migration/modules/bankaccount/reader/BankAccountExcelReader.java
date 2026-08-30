package org.egov.finance.migration.modules.bankaccount.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.bankaccount.dto.BankAccountRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BankAccountExcelReader {

	/*
	 * =========================================================
	 * EXCEL ROW CONFIGURATION
	 * =========================================================
	 *
	 * Row 1 -> Bank Account
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
	 * Column C = Bank Branch
	 * Column D = IFSC Code
	 * Column E = Account Number
	 * Column F = Fund
	 * Column G = Account Type
	 * Column H = Description
	 * Column I = Pay To
	 * Column J = Usage Type
	 *
	 * Java index:
	 *
	 * A -> 0
	 * B -> 1
	 * C -> 2
	 * D -> 3
	 * E -> 4
	 * F -> 5
	 * G -> 6
	 * H -> 7
	 * I -> 8
	 * J -> 9
	 */

	private static final int COL_SERIAL_NUMBER = 0;
	private static final int COL_ULB_NAME = 1;
	private static final int COL_BANK_BRANCH = 2;
	private static final int COL_IFSC_CODE = 3;
	private static final int COL_ACCOUNT_NUMBER = 4;
	private static final int COL_FUND = 5;
	private static final int COL_ACCOUNT_TYPE = 6;
	private static final int COL_DESCRIPTION = 7;
	private static final int COL_PAY_TO = 8;
	private static final int COL_USAGE_TYPE = 9;

	private final DataFormatter formatter = new DataFormatter();

	/**
	 * Read Bank Account Excel file.
	 */
	public List<BankAccountRecord> read(MultipartFile file) {

		List<BankAccountRecord> records = new ArrayList<>();

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
				 * Create Bank Account record.
				 */
				BankAccountRecord record =
						createBankAccountRecord(row);

				record.setStartRow(excelRowNumber);
				record.setEndRow(excelRowNumber);

				records.add(record);
			}

		} catch (Exception e) {

			throw new RuntimeException(
					"Unable to read Bank Account Excel file.", e);
		}

		return records;
	}

	/*
	 * =========================================================
	 * CREATE BANK ACCOUNT RECORD
	 * =========================================================
	 */
	private BankAccountRecord createBankAccountRecord(Row row) {

		BankAccountRecord record = new BankAccountRecord();

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
		 * Bank Branch
		 */
		record.setBranchName(
				getCellValue(row, COL_BANK_BRANCH));

		/*
		 * IFSC Code
		 */
		record.setIfscCode(
				getCellValue(row, COL_IFSC_CODE));

		/*
		 * Account Number
		 */
		record.setAccountNumber(
				getCellValue(row, COL_ACCOUNT_NUMBER));

		/*
		 * Fund
		 */
		record.setFundName(
				getCellValue(row, COL_FUND));

		/*
		 * Account Type
		 */
		record.setAccountType(
				getCellValue(row, COL_ACCOUNT_TYPE));

		/*
		 * Description
		 */
		record.setDescription(
				getCellValue(row, COL_DESCRIPTION));

		/*
		 * Pay To
		 */
		record.setPayTo(
				getCellValue(row, COL_PAY_TO));

		/*
		 * Usage Type
		 */
		record.setType(
				getCellValue(row, COL_USAGE_TYPE));

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

	    switch (cell.getCellType()) {

	    case STRING:
	        return cell.getStringCellValue().trim();

	    case NUMERIC:
	        return new java.math.BigDecimal(
	                cell.getNumericCellValue()
	        ).toBigInteger().toString();

	    case BOOLEAN:
	        return String.valueOf(
	                cell.getBooleanCellValue());

	    case FORMULA:
	        return formatter.formatCellValue(cell).trim();

	    default:
	        return "";
	    }
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
		 * Check all columns used by Bank Account.
		 *
		 * A -> Sl. No.
		 * B -> ULB Name
		 * C -> Bank Branch
		 * D -> IFSC Code
		 * E -> Account Number
		 * F -> Fund
		 * G -> Account Type
		 * H -> Description
		 * I -> Pay To
		 * J -> Usage Type
		 */

		for (int i = COL_SERIAL_NUMBER;
				i <= COL_USAGE_TYPE;
				i++) {

			if (!getCellValue(row, i).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}