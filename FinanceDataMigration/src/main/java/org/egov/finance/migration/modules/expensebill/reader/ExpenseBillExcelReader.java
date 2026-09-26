package org.egov.finance.migration.modules.expensebill.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
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

@Component
public class ExpenseBillExcelReader {

	private static final Logger log = LoggerFactory.getLogger(ExpenseBillExcelReader.class);

	/*
	 * ============================================================ SHEET
	 * CONFIGURATION ============================================================
	 */

	private static final String SHEET_NAME = "Expense Bill";

	/*
	 * Excel row 3 contains grouped headers. Excel row 4 contains actual column
	 * headers. Excel row 5 contains first data row.
	 *
	 * POI uses zero-based indexes:
	 *
	 * Excel row 5 -> POI row 4
	 */
	private static final int HEADER_GROUP_ROW = 2;
	private static final int HEADER_ROW = 3;
	private static final int DATA_START_ROW = 4;

	/*
	 * A:W = 23 columns.
	 */
	private static final int EXPECTED_COLUMN_COUNT = 23;

	/*
	 * ============================================================ MAIN BILL
	 * DETAILS ============================================================
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

	/*
	 * ============================================================ SUB LEDGER
	 * DETAILS ============================================================
	 */

	private static final int COL_SUB_LEDGER_TYPE = 12;
	private static final int COL_SUB_LEDGER_MASTER = 13;

	/*
	 * ============================================================ DEBIT DETAILS
	 * ============================================================
	 */

	private static final int COL_DEBIT_GL_CODE = 14;
	private static final int COL_DEBIT_ACCOUNT_HEAD = 15;
	private static final int COL_DEBIT_AMOUNT = 16;

	/*
	 * ============================================================ DEDUCTION
	 * DETAILS ============================================================
	 */

	private static final int COL_DEDUCTION_GL_CODE = 17;
	private static final int COL_DEDUCTION_ACCOUNT_HEAD = 18;
	private static final int COL_DEDUCTION_PERCENTAGE = 19;
	private static final int COL_DEDUCTION_CREDIT_AMOUNT = 20;

	/*
	 * ============================================================ NET PAYABLE
	 * DETAILS ============================================================
	 */

	private static final int COL_NET_PAYABLE_GL_CODE = 21;
	private static final int COL_NET_PAYABLE_CREDIT_AMOUNT = 22;

	private final DataFormatter dataFormatter = new DataFormatter();

	/**
	 * Reads Expense Bill data from an already opened Workbook.
	 *
	 * The workbook is NOT opened or closed by this reader.
	 *
	 * The caller owns the Workbook lifecycle.
	 */

	public List<ExpenseBillRecord> read(String filePath) throws Exception {

		List<ExpenseBillRecord> expenseRecords = new ArrayList<>();
		Set<Integer> serialNumbers = new HashSet<>();

		// log.info("Reading Expense Bill Excel file. FileName={}, Size={} bytes,
		// ContentType={}",file.getOriginalFilename(), file.getSize(),
		// file.getContentType());

		try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
				Workbook workbook = WorkbookFactory.create(inputStream)) {

			validateWorkbook(workbook);
			Sheet sheet = workbook.getSheet(SHEET_NAME);
			validateSheet(sheet);
//			validateHeaders(sheet);
			ExpenseBillRecord currentRecord = null;

			for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);

				/*
				 * Completely empty rows are allowed.
				 */
				if (isRowEmpty(row)) {
					continue;
				}

				int excelRowNumber = rowIndex + 1;
				validateRowStructure(row, excelRowNumber);
				boolean hasSn = hasValue(row, COL_SN);

				/*
				 * ==================================================== NEW EXPENSE BILL
				 * ====================================================
				 */
				if (hasSn) {

					Integer serialNumber = getRequiredIntegerValue(row, COL_SN, excelRowNumber, "SN");
					validateSerialNumber(serialNumber, excelRowNumber);

					if (!serialNumbers.add(serialNumber)) {
						throw new IllegalArgumentException("Duplicate SN '" + serialNumber + "' found at Excel row "
								+ excelRowNumber + ". Each Expense Bill must have a unique SN.");
					}

					currentRecord = createExpenseRecord(row, excelRowNumber);
					currentRecord.setStartRow(excelRowNumber);
					currentRecord.setEndRow(excelRowNumber);

					expenseRecords.add(currentRecord);

					log.info("New Expense Bill detected. SN={} | StartRow={}", currentRecord.getSerialNumber(),
							currentRecord.getStartRow());
				}

				/*
				 * ==================================================== CONTINUATION ROW
				 * ====================================================
				 */
				else {
					if (currentRecord == null) {
						throw new IllegalArgumentException("Invalid Excel structure at row " + excelRowNumber + ": SN is missing and no previous "
										+ "Expense Bill exists. " + "The first data row must contain an SN.");
					}

					currentRecord.setEndRow(excelRowNumber);
					validateContinuationRow(row, excelRowNumber);
				}

				/*
				 * ==================================================== TRANSACTION DETAILS
				 * ====================================================
				 */

				addDebitDetail(row, currentRecord, excelRowNumber);
				addDeductionDetail(row, currentRecord, excelRowNumber);
				addNetPayableDetail(row, currentRecord, excelRowNumber);
			}

			validateRecords(expenseRecords);

		} catch (IllegalArgumentException exception) {
			log.error("Expense Bill Excel validation failed: {}", exception.getMessage());
			throw exception;

		} catch (Exception exception) {
			log.error("Unexpected error while reading Expense Bill Excel.", exception);
			throw new RuntimeException("Unable to read Expense Bill Excel. Reason: " + getExceptionMessage(exception),
					exception);
		}

		log.info("==============================================");
		log.info("TOTAL EXPENSE BILL RECORDS READ: {}", expenseRecords.size());
		log.info("==============================================");

		for (ExpenseBillRecord record : expenseRecords) {

			log.info("SN={} | StartRow={} | EndRow={} | DebitDetails={} | DeductionDetails={} | NetPayable={} | PartyBillNo={}",
					record.getSerialNumber(), record.getStartRow(), record.getEndRow(),
					record.getDebitDetails() != null ? record.getDebitDetails().size() : 0,
					record.getDeductionDetails() != null ? record.getDeductionDetails().size() : 0,
					record.getNetPayableDetail() != null, record.getPartyBillNo());
		}

		log.info("==============================================");

		return expenseRecords;
	}

	/**
	 * Validates Workbook.
	 */
	private void validateWorkbook(Workbook workbook) {

		if (workbook == null) {
			throw new IllegalArgumentException("Unable to read Expense Bill because Excel workbook is null.");
		}

		if (workbook.getNumberOfSheets() == 0) {
			throw new IllegalArgumentException("Excel workbook does not contain any worksheet.");
		}
	}

	/**
	 * Gets and validates Expense Bill sheet.
	 */
	private void validateSheet(Sheet sheet) {

		if (sheet == null) {
			throw new IllegalArgumentException("Expense Bill worksheet '" + SHEET_NAME + "' was not found in the Excel workbook.");
		}

		if (sheet.getLastRowNum() < DATA_START_ROW) {
			throw new IllegalArgumentException("Expense Bill worksheet does not contain any data rows. "
					+ "Expected data to start from Excel row " + (DATA_START_ROW + 1) + ".");
		}

		boolean hasData = false;

		for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (!isRowEmpty(row)) {
				hasData = true;
				break;
			}
		}

		if (!hasData) {
			throw new IllegalArgumentException("Expense Bill worksheet does not contain any Expense Bill data.");
		}
	}

	/**
	 * Validates actual Excel header row.
	 */
	private void validateHeaders(Sheet sheet) {

		Row headerRow = sheet.getRow(HEADER_ROW);

		if (headerRow == null) {
			throw new IllegalArgumentException("Expense Bill header row is missing. " + "Expected headers at Excel row 4.");
		}

		String[] expectedHeaders = { "SN.", "ULB Name", "Bill Date", "Fund", "Department", "Scheme", "Fund Source",
				"Function", "Narration", "Party Bill No", "Party Bill Date", "Bill SubType",
				"Category Type (SubLedger Type)", "SubLedger Master", "GL Code (Account Code)", "Account Head",
				"Debit amount", "GL Code (Account Code)", "Account Head", "Deduction Percentage", "Credit amount",
				"Gl Code(Account Code)", "Credit Amount" };

		for (int columnIndex = 0; columnIndex < expectedHeaders.length; columnIndex++) {

			String actualHeader = getStringValue(headerRow.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));

			if (isBlank(actualHeader)) {
				throw new IllegalArgumentException("Expense Bill header is missing at Excel row 4, " + "column " + getColumnName(columnIndex) + ".");
			}

			/*
			 * Do not require exact header text because the template contains mandatory '*'
			 * and date descriptions.
			 */
			String normalizedActual = normalizeHeader(actualHeader);
			String normalizedExpected = normalizeHeader(expectedHeaders[columnIndex]);

			if (!normalizedActual.contains(normalizedExpected) && !normalizedExpected.contains(normalizedActual)) {
				log.warn("Expense Bill header differs at column {}. " + "Expected='{}', Actual='{}'",
						getColumnName(columnIndex), expectedHeaders[columnIndex], actualHeader);
			}
		}

		log.info("Expense Bill Excel headers validated successfully.");
	}

	/**
	 * Validates row structure.
	 */
	private void validateRowStructure(Row row, int excelRowNumber) {

		if (row == null) {
			throw new IllegalArgumentException("Invalid empty row at Excel row " + excelRowNumber + ".");
		}

		/*
		 * We only require A:W.
		 *
		 * The uploaded template has additional blank formatting columns after W, so we
		 * intentionally ignore them.
		 */
		for (int columnIndex = 0; columnIndex < EXPECTED_COLUMN_COUNT; columnIndex++) {

			/*
			 * No need to reject missing physical cells here. Individual field validation
			 * handles required values.
			 */
			row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
		}
	}

	/**
	 * Validates continuation row.
	 */
	private void validateContinuationRow(Row row, int excelRowNumber) {

		boolean hasDebitData = hasAnyValue(row, COL_DEBIT_GL_CODE, COL_DEBIT_ACCOUNT_HEAD, COL_DEBIT_AMOUNT);
		boolean hasDeductionData = hasAnyValue(row, COL_DEDUCTION_GL_CODE, COL_DEDUCTION_ACCOUNT_HEAD,COL_DEDUCTION_PERCENTAGE, COL_DEDUCTION_CREDIT_AMOUNT);
		boolean hasNetPayableData = hasAnyValue(row, COL_NET_PAYABLE_GL_CODE, COL_NET_PAYABLE_CREDIT_AMOUNT);

		if (!hasDebitData && !hasDeductionData && !hasNetPayableData) {
			throw new IllegalArgumentException("Invalid continuation row at Excel row " + excelRowNumber + ": SN is empty and the row does not contain " + "Debit, Deduction or Net Payable details.");
		}
	}

	/**
	 * Creates main Expense Bill record.
	 */
	private ExpenseBillRecord createExpenseRecord(Row row, int excelRowNumber) {

		ExpenseBillRecord record = new ExpenseBillRecord();

		record.setSerialNumber(getRequiredIntegerValue(row, COL_SN, excelRowNumber, "SN"));
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
	 * Adds Debit Detail.
	 */
	private void addDebitDetail(Row row, ExpenseBillRecord expenseRecord, int excelRowNumber) {

		String glCode = getStringValue(row.getCell(COL_DEBIT_GL_CODE));
		String accountHead = getStringValue(row.getCell(COL_DEBIT_ACCOUNT_HEAD));
		boolean hasAmount = hasValue(row, COL_DEBIT_AMOUNT);
		BigDecimal debitAmount = getBigDecimalValue(row.getCell(COL_DEBIT_AMOUNT), excelRowNumber, "Debit Amount");

		if (isBlank(glCode) && isBlank(accountHead) && !hasAmount) {
			return;
		}

		if (isBlank(glCode)) {
			throw new IllegalArgumentException("Debit GL Code is missing at Excel row " + excelRowNumber + ". Debit Amount="
							+ formatValue(debitAmount) + ", Account Head=" + formatValue(accountHead) + ".");
		}

		if (debitAmount == null) {
			throw new IllegalArgumentException("Debit Amount is missing at Excel row " + excelRowNumber + " for Debit GL Code '" + glCode + "'.");
		}

		validatePositiveAmount(debitAmount, "Debit Amount", excelRowNumber, glCode);

		ExpenseDebitRecord debitRecord = new ExpenseDebitRecord();

		debitRecord.setGlCode(glCode);
		debitRecord.setAccountHead(accountHead);
		debitRecord.setDebitAmount(debitAmount);

		expenseRecord.getDebitDetails().add(debitRecord);
	}

	/**
	 * Adds Deduction Detail.
	 */
	private void addDeductionDetail(Row row, ExpenseBillRecord expenseRecord, int excelRowNumber) {

		String glCode = getStringValue(row.getCell(COL_DEDUCTION_GL_CODE));
		String accountHead = getStringValue(row.getCell(COL_DEDUCTION_ACCOUNT_HEAD));
		boolean hasPercentage = hasValue(row, COL_DEDUCTION_PERCENTAGE);
		boolean hasCreditAmount = hasValue(row, COL_DEDUCTION_CREDIT_AMOUNT);
		BigDecimal percentage = getBigDecimalValue(row.getCell(COL_DEDUCTION_PERCENTAGE), excelRowNumber, "Deduction Percentage");
		BigDecimal creditAmount = getBigDecimalValue(row.getCell(COL_DEDUCTION_CREDIT_AMOUNT), excelRowNumber, "Deduction Credit Amount");

		if (isBlank(glCode) && isBlank(accountHead) && !hasPercentage && !hasCreditAmount) {
			return;
		}

		if (isBlank(glCode)) {
			throw new IllegalArgumentException("Deduction GL Code is missing at Excel row " + excelRowNumber + ".");
		}

		if (creditAmount == null) {
			throw new IllegalArgumentException("Deduction Credit Amount is missing at Excel row " + excelRowNumber + " for Deduction GL Code '" + glCode + "'.");
		}

		validatePositiveAmount(creditAmount, "Deduction Credit Amount", excelRowNumber, glCode);

		if (percentage != null && (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0)) {
			throw new IllegalArgumentException("Invalid Deduction Percentage '" + percentage + "' at Excel row " + excelRowNumber + " for GL Code '" + glCode + "'. Percentage must be between 0 and 100.");
		}

		ExpenseDeductionRecord deductionRecord = new ExpenseDeductionRecord();
		deductionRecord.setGlCode(glCode);
		deductionRecord.setAccountHead(accountHead);
		deductionRecord.setDeductionPercentage(percentage);
		deductionRecord.setCreditAmount(creditAmount);
		expenseRecord.getDeductionDetails().add(deductionRecord);
	}

	/**
	 * Adds Net Payable Detail.
	 */
	private void addNetPayableDetail(Row row, ExpenseBillRecord expenseRecord, int excelRowNumber) {

		String glCode = getStringValue(row.getCell(COL_NET_PAYABLE_GL_CODE));
		boolean hasCreditAmount = hasValue(row, COL_NET_PAYABLE_CREDIT_AMOUNT);
		BigDecimal creditAmount = getBigDecimalValue(row.getCell(COL_NET_PAYABLE_CREDIT_AMOUNT), excelRowNumber,"Net Payable Credit Amount");

		if (isBlank(glCode) && !hasCreditAmount) {
			return;
		}

		if (isBlank(glCode)) {
			throw new IllegalArgumentException("Net Payable GL Code is missing at Excel row " + excelRowNumber + ".");
		}

		if (creditAmount == null) {
			throw new IllegalArgumentException("Net Payable Credit Amount is missing at Excel row " + excelRowNumber
					+ " for Net Payable GL Code '" + glCode + "'.");
		}

		validatePositiveAmount(creditAmount, "Net Payable Credit Amount", excelRowNumber, glCode);

		if (expenseRecord.getNetPayableDetail() != null) {
			throw new IllegalArgumentException("Multiple Net Payable details found for Expense Bill SN " + expenseRecord.getSerialNumber()
							+ ". Another Net Payable was found at Excel row " + excelRowNumber + ".");
		}

		ExpenseNetPayableRecord netPayableRecord = new ExpenseNetPayableRecord();

		netPayableRecord.setGlCode(glCode);
		netPayableRecord.setCreditAmount(creditAmount);
		expenseRecord.setNetPayableDetail(netPayableRecord);
	}

	/**
	 * Final validation after grouping all rows.
	 */
	private void validateRecords(List<ExpenseBillRecord> records) {

		if (records == null || records.isEmpty()) {
			throw new IllegalArgumentException("No valid Expense Bill records were found " + "in the Excel file.");
		}

		for (ExpenseBillRecord record : records) {

			if (record == null) {
				throw new IllegalArgumentException("Excel processing produced a null " + "Expense Bill record.");
			}

			Integer sn = record.getSerialNumber();

			if (sn == null || sn <= 0) {
				throw new IllegalArgumentException("Invalid SN for Expense Bill at Excel rows " + record.getStartRow()+ "-" + record.getEndRow() + ". SN must be greater than zero.");
			}

			if (record.getStartRow() <= 0 || record.getEndRow() < record.getStartRow()) {
				throw new IllegalArgumentException("Invalid row range for Expense Bill SN " + sn + ": StartRow=" + record.getStartRow() + ", EndRow=" + record.getEndRow() + ".");
			}

			if (record.getDebitDetails() == null || record.getDebitDetails().isEmpty()) {
				throw new IllegalArgumentException("No Debit Details found for Expense Bill SN " + sn + " at Excel rows " + record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			if (record.getNetPayableDetail() == null) {
				throw new IllegalArgumentException("Net Payable Detail is missing for Expense Bill SN " + sn + " at Excel rows " + record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			validateRecordDetails(record);
		}
	}

	/**
	 * Validates details of one Expense Bill.
	 */
	private void validateRecordDetails(ExpenseBillRecord record) {

		int sn = record.getSerialNumber();

		for (int i = 0; i < record.getDebitDetails().size(); i++) {

			ExpenseDebitRecord debit = record.getDebitDetails().get(i);
			if (debit == null) {
				throw new IllegalArgumentException("Null Debit Detail found for Expense Bill SN " + sn + " at Debit Detail index " + i + ".");
			}

			if (isBlank(debit.getGlCode())) {
				throw new IllegalArgumentException("Debit GL Code is missing for Expense Bill SN " + sn + " at Debit Detail index " + i + ".");
			}

			if (debit.getDebitAmount() == null) {
				throw new IllegalArgumentException("Debit Amount is missing for Expense Bill SN " + sn + ", GL Code '" + debit.getGlCode() + "'.");
			}

			validatePositiveAmount(debit.getDebitAmount(), "Debit Amount", record.getStartRow(), debit.getGlCode());
		}

		if (record.getDeductionDetails() != null) {

			for (int i = 0; i < record.getDeductionDetails().size(); i++) {
				ExpenseDeductionRecord deduction = record.getDeductionDetails().get(i);

				if (deduction == null) {
					throw new IllegalArgumentException("Null Deduction Detail found for Expense Bill SN " + sn + " at Deduction Detail index " + i + ".");
				}

				if (isBlank(deduction.getGlCode())) {
					throw new IllegalArgumentException("Deduction GL Code is missing for Expense Bill SN " + sn + " at Deduction Detail index " + i + ".");
				}

				if (deduction.getCreditAmount() == null) {
					throw new IllegalArgumentException("Deduction Credit Amount is missing for Expense Bill SN " + sn + ", GL Code '" + deduction.getGlCode() + "'.");
				}
			}
		}

		ExpenseNetPayableRecord netPayable = record.getNetPayableDetail();

		if (netPayable == null) {
			throw new IllegalArgumentException("Net Payable Detail is missing for Expense Bill SN " + sn + ".");
		}

		if (isBlank(netPayable.getGlCode())) {
			throw new IllegalArgumentException("Net Payable GL Code is missing for Expense Bill SN " + sn + ".");
		}

		if (netPayable.getCreditAmount() == null) {
			throw new IllegalArgumentException("Net Payable Credit Amount is missing for Expense Bill SN " + sn	+ ", GL Code '" + netPayable.getGlCode() + "'.");
		}
	}

	/**
	 * Reads required Integer.
	 */
	private Integer getRequiredIntegerValue(Row row, int columnIndex, int excelRowNumber, String fieldName) {

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			throw new IllegalArgumentException(fieldName + " is missing at Excel row " + excelRowNumber + ".");
		}

		String rawValue = getStringValue(cell);

		if (isBlank(rawValue)) {
			throw new IllegalArgumentException(fieldName + " is blank at Excel row " + excelRowNumber + ".");
		}

		BigDecimal decimalValue;

		try {

			decimalValue = new BigDecimal(rawValue.replace(",", "").trim());

		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid " + fieldName + " '" + rawValue + "' at Excel row "	+ excelRowNumber + ". Expected a whole number.", exception);
		}

		if (decimalValue.stripTrailingZeros().scale() > 0) {
			throw new IllegalArgumentException("Invalid " + fieldName + " '" + rawValue + "' at Excel row "	+ excelRowNumber + ". " + fieldName + " must be a whole number.");
		}

		try {
			return decimalValue.intValueExact();
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Invalid " + fieldName + " '" + rawValue + "' at Excel row "	+ excelRowNumber + ". Value is outside the supported integer range.", exception);
		}
	}

	/**
	 * Reads BigDecimal.
	 */
	private BigDecimal getBigDecimalValue(Cell cell, int excelRowNumber, String fieldName) {

		if (cell == null) {
			return null;
		}

		String value = getStringValue(cell);

		if (isBlank(value)) {
			return null;
		}

		String normalizedValue = value.replace(",", "").trim();

		try {
			BigDecimal amount = new BigDecimal(normalizedValue);
			if (amount.scale() > 10) {
				throw new IllegalArgumentException("Invalid " + fieldName + " '" + value + "' at Excel row " + excelRowNumber + ". Decimal precision is too high.");
			}

			return amount;

		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid " + fieldName + " '" + value + "' at Excel row " + excelRowNumber + ". Expected a valid numeric value.", exception);
		}
	}

	/**
	 * Validates positive amount.
	 */
	private void validatePositiveAmount(BigDecimal amount, String fieldName, int excelRowNumber, String glCode) {

		if (amount == null) {
			throw new IllegalArgumentException(fieldName + " is missing at Excel row " + excelRowNumber + ".");
		}

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Invalid " + fieldName + " '" + amount + "' at Excel row "
					+ excelRowNumber + " for GL Code '" + glCode + "'. Amount must be greater than zero.");
		}
	}

	/**
	 * Validates SN.
	 */
	private void validateSerialNumber(Integer serialNumber, int excelRowNumber) {

		if (serialNumber == null) {
			throw new IllegalArgumentException("SN is missing at Excel row " + excelRowNumber + ".");
		}

		if (serialNumber <= 0) {
			throw new IllegalArgumentException("Invalid SN '" + serialNumber + "' at Excel row " + excelRowNumber + ". SN must be greater than zero.");
		}
	}

	/**
	 * Checks whether specified columns contain any value.
	 */
	private boolean hasAnyValue(Row row, int... columnIndexes) {

		if (row == null || columnIndexes == null) {
			return false;
		}

		for (int columnIndex : columnIndexes) {
			if (hasValue(row, columnIndex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a cell has value.
	 */
	private boolean hasValue(Row row, int columnIndex) {

		if (row == null) {
			return false;
		}

		String value = getStringValue(row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));
		return !isBlank(value);
	}

	/**
	 * Checks whether row is completely empty.
	 */
	private boolean isRowEmpty(Row row) {

		if (row == null) {
			return true;
		}

		short firstCellNum = row.getFirstCellNum();
		short lastCellNum = row.getLastCellNum();

		if (firstCellNum < 0 || lastCellNum < 0) {
			return true;
		}

		for (int cellIndex = firstCellNum; cellIndex < lastCellNum; cellIndex++) {
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
	 * Normalizes header for comparison.
	 */
	private String normalizeHeader(String header) {

		if (header == null) {
			return "";
		}

		return header.replace("*", "").replace("\n", " ").replaceAll("\\s+", " ").trim().toLowerCase();
	}

	/**
	 * Returns Excel column name.
	 */
	private String getColumnName(int columnIndex) {

		StringBuilder columnName = new StringBuilder();
		int index = columnIndex + 1;

		while (index > 0) {

			int remainder = (index - 1) % 26;
			columnName.insert(0, (char) ('A' + remainder));
			index = (index - 1) / 26;
		}

		return columnName.toString();
	}

	/**
	 * Formats value for error message.
	 */
	private String formatValue(Object value) {
		return value == null ? "<blank>" : "'" + value + "'";
	}

	/**
	 * Checks blank string.
	 */
	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Extracts meaningful exception message.
	 */
	private String getExceptionMessage(Throwable exception) {

		if (exception == null) {
			return "Unknown error";
		}

		Throwable current = exception;

		while (current != null) {

			if (!isBlank(current.getMessage())) {
				return current.getMessage();
			}

			current = current.getCause();
		}

		return exception.getClass().getSimpleName();
	}
}