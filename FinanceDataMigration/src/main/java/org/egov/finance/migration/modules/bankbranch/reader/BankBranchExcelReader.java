package org.egov.finance.migration.modules.bankbranch.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.bankbranch.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BankBranchExcelReader {

	/*
	 * =========================================================
	 * EXCEL ROW CONFIGURATION
	 * =========================================================
	 *
	 * Row 1 -> Bank Branch Master
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
	 * Column C = Bank
	 * Column D = Branch Name/Location
	 * Column E = IFSC Code
	 * Column F = Branch Code
	 * Column G = MICR
	 * Column H = Address
	 * Column I = Contact Person
	 * Column J = Phone Number
	 * Column K = Narration
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
	 * K -> 10
	 */

	private static final int COL_SERIAL_NUMBER = 0;
	private static final int COL_ULB_NAME = 1;
	private static final int COL_BANK_NAME = 2;
	private static final int COL_BRANCH_NAME = 3;
	private static final int COL_IFSC_CODE = 4;
	private static final int COL_BRANCH_CODE = 5;
	private static final int COL_MICR = 6;
	private static final int COL_ADDRESS = 7;
	private static final int COL_CONTACT_PERSON = 8;
	private static final int COL_PHONE_NUMBER = 9;
	private static final int COL_NARRATION = 10;

	private final DataFormatter formatter = new DataFormatter();

	/**
	 * Read Bank Branch Master Excel file.
	 */
	public List<BankBranchRecord> read(MultipartFile file) {

		List<BankBranchRecord> records = new ArrayList<>();

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
				 * Create Bank Branch record.
				 */
				BankBranchRecord record = createBankBranchRecord(row);

				record.setStartRow(excelRowNumber);
				record.setEndRow(excelRowNumber);

				records.add(record);
			}

		} catch (Exception e) {

			throw new RuntimeException(
					"Unable to read Bank Branch Master Excel file.", e);
		}

		return records;
	}

	/*
	 * =========================================================
	 * CREATE BANK BRANCH RECORD
	 * =========================================================
	 */
	private BankBranchRecord createBankBranchRecord(Row row) {

		BankBranchRecord record = new BankBranchRecord();

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
		 * Branch Name / Location
		 */
		record.setBranchName(
				getCellValue(row, COL_BRANCH_NAME));

		/*
		 * IFSC Code
		 */
		record.setIfscCode(
				getCellValue(row, COL_IFSC_CODE));

		/*
		 * Branch Code
		 */
		record.setBranchCode(
				getCellValue(row, COL_BRANCH_CODE));

		/*
		 * MICR
		 */
		record.setMicr(
				getCellValue(row, COL_MICR));

		/*
		 * Address
		 */
		record.setAddress(
				getCellValue(row, COL_ADDRESS));

		/*
		 * Contact Person
		 */
		record.setContactPerson(
				getCellValue(row, COL_CONTACT_PERSON));

		/*
		 * Phone Number
		 */
		record.setPhoneNumber(
				getCellValue(row, COL_PHONE_NUMBER));

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
		 * Check all columns used by Bank Branch Master.
		 *
		 * A -> Sl. No.
		 * B -> ULB Name
		 * C -> Bank Name
		 * D -> Branch Name
		 * E -> IFSC Code
		 * F -> Branch Code
		 * G -> MICR
		 * H -> Address
		 * I -> Contact Person
		 * J -> Phone Number
		 * K -> Narration
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