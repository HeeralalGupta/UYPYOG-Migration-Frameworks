package org.egov.finance.migration.modules.expensebill.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRecord;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseDebitRecord;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseDeductionRecord;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseNetPayableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ExpenseBillExcelReader {

	Logger log = LoggerFactory.getLogger(ExpenseBillExcelReader.class);
	/*
	 * Excel column indexes
	 */
	private static final int COL_SN = 0;
	private static final int COL_ULB_NAME = 1;
	private static final int COL_BILL_DATE = 2;
	private static final int COL_FUND = 3;
	private static final int COL_DEPARTMENT = 4;
	private static final int COL_SCHEME = 5;
	private static final int COL_FUND_SOURCE = 6;
	private static final int COL_FUNCTION = 7;
	private static final int COL_NARRATION = 8;
	private static final int COL_PARTY_BILL_NO = 9;
	private static final int COL_PARTY_BILL_DATE = 10;
	private static final int COL_BILL_SUB_TYPE = 11;

	private static final int COL_SUB_LEDGER_TYPE = 12;
	private static final int COL_SUB_LEDGER_MASTER = 13;

	/*
	 * Debit Details
	 */
	private static final int COL_DEBIT_GL_CODE = 14;
	private static final int COL_DEBIT_ACCOUNT_HEAD = 15;
	private static final int COL_DEBIT_AMOUNT = 16;

	/*
	 * Deduction Details
	 */
	private static final int COL_DEDUCTION_GL_CODE = 17;
	private static final int COL_DEDUCTION_ACCOUNT_HEAD = 18;
	private static final int COL_DEDUCTION_PERCENTAGE = 19;
	private static final int COL_DEDUCTION_CREDIT_AMOUNT = 20;

	/*
	 * Net Payable Details
	 */
	private static final int COL_NET_PAYABLE_GL_CODE = 21;
	private static final int COL_NET_PAYABLE_CREDIT_AMOUNT = 22;

	/*
	 * Update this according to your template.
	 *
	 * If row 0 to row 3 contains title/header information and actual data starts
	 * from row 4.
	 */
	private static final int DATA_START_ROW = 4;
	private final DataFormatter dataFormatter = new DataFormatter();

	/**
	 * Reads Expense Bill Excel file.
	 *
	 * Multiple rows with blank SN belong to the previous ExpenseRecord.
	 */

	public List<ExpenseBillRecord> read(MultipartFile file) throws Exception {

		List<ExpenseBillRecord> expenseRecords = new ArrayList<>();

		if (file == null) {
			throw new IllegalArgumentException("Excel file is required.");
		}

		if (file.isEmpty()) {
			throw new IllegalArgumentException("Uploaded Excel file is empty.");
		}

		log.info("File name: {}", file.getOriginalFilename());
		log.info("Content type: {}", file.getContentType());
		log.info("File size: {} bytes", file.getSize());

		try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);

			ExpenseBillRecord currentRecord = null;

			for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				if (isRowEmpty(row)) {
					continue;
				}

				/*
				 * Convert Apache POI row index to actual Excel row number.
				 *
				 * POI index 4 = Excel Row 5
				 */
				int excelRowNumber = rowIndex + 1;

				/*
				 * If SN is present, create a new Expense Bill.
				 */
				if (hasValue(row, COL_SN)) {

					currentRecord = createExpenseRecord(row);

					/*
					 * Set row tracking for migration and duplicate detection.
					 */
					currentRecord.setStartRow(excelRowNumber);
					currentRecord.setEndRow(excelRowNumber);

					expenseRecords.add(currentRecord);

					log.info("New Expense Bill detected. SN={} | StartRow={}", currentRecord.getSerialNumber(),
							currentRecord.getStartRow());

				} else if (currentRecord == null) {

					/*
					 * Ignore rows before first valid bill row.
					 */
					log.warn("Ignoring row {} because no Expense Bill SN found.", excelRowNumber);

					continue;

				} else {

					/*
					 * Continuation row belongs to the current Expense Bill.
					 */
					currentRecord.setEndRow(excelRowNumber);
				}

				/*
				 * Add Debit Detail
				 */
				addDebitDetail(row, currentRecord);

				/*
				 * Add Deduction Detail
				 */
				addDeductionDetail(row, currentRecord);

				/*
				 * Set Net Payable Detail
				 */
				addNetPayableDetail(row, currentRecord);
			}

		} catch (Exception e) {

			log.error("Unable to read Expense Bill Excel file.", e);

			throw new RuntimeException("Unable to read Expense Bill Excel file.", e);
		}

		/*
		 * Debug all grouped records.
		 */
		log.info("==============================================");
		log.info("TOTAL EXPENSE BILL RECORDS READ: {}", expenseRecords.size());
		log.info("==============================================");

		expenseRecords.forEach(record -> log.info(
				"SN={} | StartRow={} | EndRow={} | DebitDetails={} | DeductionDetails={} | PartyBillNo={}",
				record.getSerialNumber(), record.getStartRow(), record.getEndRow(),
				record.getDebitDetails() != null ? record.getDebitDetails().size() : 0,
				record.getDeductionDetails() != null ? record.getDeductionDetails().size() : 0,
				record.getPartyBillNo()));

		log.info("==============================================");

		return expenseRecords;
	}

	/**
	 * Creates main ExpenseRecord.
	 */
	private ExpenseBillRecord createExpenseRecord(Row row) {

		ExpenseBillRecord record = new ExpenseBillRecord();

		record.setSerialNumber(getIntegerValue(row.getCell(COL_SN)));
		record.setUlbName(getStringValue(row.getCell(COL_ULB_NAME)));
		record.setBillDate(getStringValue(row.getCell(COL_BILL_DATE)));
		record.setFund(getStringValue(row.getCell(COL_FUND)));
		record.setDepartment(getStringValue(row.getCell(COL_DEPARTMENT)));
		record.setScheme(getStringValue(row.getCell(COL_SCHEME)));
		record.setFundSource(getStringValue(row.getCell(COL_FUND_SOURCE)));
		record.setFunction(getStringValue(row.getCell(COL_FUNCTION)));
		record.setNarration(getStringValue(row.getCell(COL_NARRATION)));
		record.setPartyBillNo(getStringValue(row.getCell(COL_PARTY_BILL_NO)));
		record.setPartyBillDate(getStringValue(row.getCell(COL_PARTY_BILL_DATE)));
		record.setBillSubType(getStringValue(row.getCell(COL_BILL_SUB_TYPE)));
		record.setSubLedgerType(getStringValue(row.getCell(COL_SUB_LEDGER_TYPE)));
		record.setSubLedgerMaster(getStringValue(row.getCell(COL_SUB_LEDGER_MASTER)));

		return record;
	}

	/**
	 * Adds Debit Detail if debit columns contain data.
	 */
	private void addDebitDetail(Row row, ExpenseBillRecord expenseRecord) {

		String glCode = getStringValue(row.getCell(COL_DEBIT_GL_CODE));
		String accountHead = getStringValue(row.getCell(COL_DEBIT_ACCOUNT_HEAD));
		BigDecimal debitAmount = getBigDecimalValue(row.getCell(COL_DEBIT_AMOUNT));

		/*
		 * Ignore completely empty debit rows.
		 */
		if (isBlank(glCode) && isBlank(accountHead) && debitAmount == null) {
			return;
		}

		ExpenseDebitRecord debitRecord = new ExpenseDebitRecord();

		debitRecord.setGlCode(glCode);
		debitRecord.setAccountHead(accountHead);
		debitRecord.setDebitAmount(debitAmount);
		expenseRecord.getDebitDetails().add(debitRecord);
	}

	/**
	 * Adds Deduction Detail if deduction columns contain data.
	 */
	private void addDeductionDetail(Row row, ExpenseBillRecord expenseRecord) {
		String glCode = getStringValue(row.getCell(COL_DEDUCTION_GL_CODE));
		String accountHead = getStringValue(row.getCell(COL_DEDUCTION_ACCOUNT_HEAD));
		BigDecimal percentage = getBigDecimalValue(row.getCell(COL_DEDUCTION_PERCENTAGE));
		BigDecimal creditAmount = getBigDecimalValue(row.getCell(COL_DEDUCTION_CREDIT_AMOUNT));

		/*
		 * Ignore completely empty deduction rows.
		 */
		if (isBlank(glCode) && isBlank(accountHead) && percentage == null && creditAmount == null) {

			return;
		}

		ExpenseDeductionRecord deductionRecord = new ExpenseDeductionRecord();

		deductionRecord.setGlCode(glCode);
		deductionRecord.setAccountHead(accountHead);
		deductionRecord.setDeductionPercentage(percentage);
		deductionRecord.setCreditAmount(creditAmount);
		expenseRecord.getDeductionDetails().add(deductionRecord);
	}

	/**
	 * Sets Net Payable Detail.
	 */
	private void addNetPayableDetail(Row row, ExpenseBillRecord expenseRecord) {

		String glCode = getStringValue(row.getCell(COL_NET_PAYABLE_GL_CODE));
		BigDecimal creditAmount = getBigDecimalValue(row.getCell(COL_NET_PAYABLE_CREDIT_AMOUNT));

		/*
		 * Ignore if both values are empty.
		 */
		if (isBlank(glCode) && creditAmount == null) {

			return;
		}

		ExpenseNetPayableRecord netPayableRecord = new ExpenseNetPayableRecord();
		netPayableRecord.setGlCode(glCode);
		netPayableRecord.setCreditAmount(creditAmount);
		expenseRecord.setNetPayableDetail(netPayableRecord);
	}

	/**
	 * Checks whether a specific column has value.
	 */
	private boolean hasValue(Row row, int columnIndex) {

		if (row == null) {
			return false;
		}

		String value = getStringValue(row.getCell(columnIndex));
		return !isBlank(value);
	}

	/**
	 * Checks whether complete row is empty.
	 */
	private boolean isRowEmpty(Row row) {

		if (row == null) {
			return true;
		}

		for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
			Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
			if (cell == null) {
				continue;
			}

			String value = getStringValue(cell);
			if (!isBlank(value)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Reads cell as String.
	 *
	 * DataFormatter preserves values such as: 3501000003 01001 EXP-001
	 */
	private String getStringValue(Cell cell) {

		if (cell == null) {
			return null;
		}

		String value = dataFormatter.formatCellValue(cell);

		if (value == null) {
			return null;
		}

		value = value.trim();
		return value.isEmpty() ? null : value;
	}

	/**
	 * Reads Excel Date.
	 */
//	private Date getDateValue(Cell cell) {
//
//		if (cell == null) {
//			return null;
//		}
//
//		if (cell.getCellType() == CellType.STRING && DateUtil.isCellDateFormatted(cell)) {
//			return cell.getDateCellValue();
//		}
//		return null;
//	}

	/**
	 * Reads numeric values as BigDecimal.
	 */
	private BigDecimal getBigDecimalValue(Cell cell) {

		if (cell == null) {
			return null;
		}

		if (cell.getCellType() == CellType.NUMERIC) {
			return BigDecimal.valueOf(cell.getNumericCellValue());
		}

		String value = getStringValue(cell);

		if (isBlank(value)) {
			return null;
		}

		/*
		 * Remove comma separators if Excel contains: 1,00,000.00
		 */
		value = value.replace(",", "");

		try {
			return new BigDecimal(value);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	/**
	 * Reads integer value.
	 */
	private Integer getIntegerValue(Cell cell) {
		BigDecimal value = getBigDecimalValue(cell);
		return value != null ? value.intValue() : null;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}