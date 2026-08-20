package org.egov.finance.migration.modules.journalvoucher.reader;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.journalvoucher.dto.JournalVoucherLedger;
import org.egov.finance.migration.modules.journalvoucher.dto.JournalVoucherRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class JournalVoucherExcelReader {

	private static final int HEADER_ROW_1 = 2;
	private static final int HEADER_ROW_2 = 3;

	private static final int DATA_START_ROW = 4;

	private final DataFormatter formatter = new DataFormatter();

	public List<JournalVoucherRecord> read(MultipartFile file) {
		List<JournalVoucherRecord> records = new ArrayList<>();
		try (InputStream inputStream = file.getInputStream();
				Workbook workbook = WorkbookFactory.create(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);
			JournalVoucherRecord currentRecord = null;
			for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null || isEmptyRow(row)) {
					continue;
				}

				/*
				 * Excel row number for user.
				 *
				 * POI row index starts from 0.
				 */
				int excelRowNumber = rowIndex + 1;

				/*
				 * Check whether this row starts a new voucher.
				 */
				boolean newVoucher = isNewVoucherRow(row);

				if (newVoucher) {

					/*
					 * Save previous voucher
					 */
					if (currentRecord != null) {
						currentRecord.setEndRow(excelRowNumber - 1);
						records.add(currentRecord);
					}

					/*
					 * Start new voucher
					 */
					currentRecord = createVoucherRecord(row);
					currentRecord.setStartRow(excelRowNumber);
				}

				/*
				 * Safety check
				 */
				if (currentRecord == null) {
					continue;
				}

				/*
				 * Read account / ledger row
				 */
				JournalVoucherLedger ledger = readLedger(row);

				if (ledger != null) {
					currentRecord.getLedgers().add(ledger);
				}

				/*
				 * Keep updating end row.
				 */
				currentRecord.setEndRow(excelRowNumber);
			}

			/*
			 * Add last voucher
			 */
			if (currentRecord != null) {
				records.add(currentRecord);
			}

		} catch (Exception e) {
			throw new RuntimeException("Unable to read Journal Voucher Excel file.", e);
		}

		return records;
	}

	/*
	 * ========================================================= NEW VOUCHER
	 * DETECTION =========================================================
	 *
	 * A new voucher begins when voucher-level information is present.
	 *
	 * Example:
	 *
	 * Row 5 -> ULB, Date, VoucherName, etc. Row 6 -> only S.No + GL + Amounts
	 *
	 * Row 7 -> ULB, Date, VoucherName, etc. => NEW VOUCHER
	 */
	private boolean isNewVoucherRow(Row row) {

		return !getCellValue(row, 1).isEmpty() || !getCellValue(row, 2).isEmpty() || !getCellValue(row, 3).isEmpty()
				|| !getCellValue(row, 4).isEmpty() || !getCellValue(row, 5).isEmpty() || !getCellValue(row, 7).isEmpty()
				|| !getCellValue(row, 13).isEmpty();
	}

	/*
	 * ========================================================= CREATE VOUCHER
	 * RECORD =========================================================
	 */
	private JournalVoucherRecord createVoucherRecord(Row row) {

		JournalVoucherRecord record = new JournalVoucherRecord();

		record.setUlbName(getCellValue(row, 1));
		record.setVoucherDate(getCellValue(row, 2));
		record.setVoucherName(getCellValue(row, 3));
		record.setVoucherType(getCellValue(row, 4));
		record.setDepartment(getCellValue(row, 5));
		record.setDepartmentOther(getCellValue(row, 6));
		record.setFund(getCellValue(row, 7));
		record.setFundOther(getCellValue(row, 8));
		record.setFunction(getCellValue(row, 9));
		record.setScheme(getCellValue(row, 10));
		record.setSubScheme(getCellValue(row, 11));
		record.setSource(getCellValue(row, 12));
		record.setDescription(getCellValue(row, 13));
		record.setServiceName(getCellValue(row, 14));

		return record;
	}

	/*
	 * ========================================================= READ LEDGER
	 * =========================================================
	 *
	 * Excel:
	 *
	 * Column 16 = GLCode Column 17 = DebitAmount Column 18 = CreditAmount Column 19
	 * = LedgerFunctionCode Column 20 = DetailType Column 21 = DetailKey Column 22 =
	 * SubledgerAmount
	 *
	 * Java index:
	 *
	 * 15,16,17,18,19,20,21
	 */
	private JournalVoucherLedger readLedger(Row row) {

		String glCode = getCellValue(row, 15);
		String debit = getCellValue(row, 16);
		String credit = getCellValue(row, 17);

		/*
		 * If there is no GL code and no amount, this isn't a ledger row.
		 */
		if (glCode.isEmpty() && debit.isEmpty() && credit.isEmpty()) {

			return null;
		}

		JournalVoucherLedger ledger = new JournalVoucherLedger();

		ledger.setGlCode(glCode);
		ledger.setDebitAmount(parseDouble(debit));
		ledger.setCreditAmount(parseDouble(credit));
		ledger.setLedgerFunctionCode(getCellValue(row, 18));
		ledger.setDetailType(getCellValue(row, 19));
		ledger.setDetailKey(getCellValue(row, 20));
		ledger.setSubledgerAmount(parseDouble(getCellValue(row, 21)));

		return ledger;
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
	 * ========================================================= DOUBLE
	 * =========================================================
	 */
	private Double parseDouble(String value) {

		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			return Double.parseDouble(value.replace(",", ""));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/*
	 * ========================================================= EMPTY ROW
	 * =========================================================
	 */
	private boolean isEmptyRow(Row row) {

		for (int i = 0; i < row.getLastCellNum(); i++) {
			if (!getCellValue(row, i).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}
