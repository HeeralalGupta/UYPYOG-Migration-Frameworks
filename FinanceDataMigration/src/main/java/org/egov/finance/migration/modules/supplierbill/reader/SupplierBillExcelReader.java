package org.egov.finance.migration.modules.supplierbill.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.supplierbill.dto.EgBillDetails;
import org.egov.finance.migration.modules.supplierbill.dto.SupplierBillRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SupplierBillExcelReader {

	private static final Logger log = LoggerFactory.getLogger(SupplierBillExcelReader.class);

	/*
	 * ========================================================= EXCEL COLUMN
	 * INDEXES =========================================================
	 *
	 * B = SN C = ULB Name D = Bill Date E = Supplier F = Purchase Order G = Fund H
	 * = Department I = Scheme J = Sub Scheme K = Fund Source L = Function M =
	 * Narration N = Party Bill No O = Party Bill Date P = Bill Type
	 *
	 * Q = Debit GL Code R = Debit Account Head S = Debit Amount
	 *
	 * T = Deduction GL Code U = Deduction Account Head V = Deduction Percentage W =
	 * Deduction Credit Amount
	 *
	 * X = Net Payable GL Code Y = Net Payable Credit Amount
	 *
	 * Java indexes: B = 1 C = 2 ... Y = 24
	 */

	private static final int COL_SN = 1;
	private static final int COL_ULB_NAME = 2;
	private static final int COL_BILL_DATE = 3;
	private static final int COL_SUPPLIER = 4;
	private static final int COL_PURCHASE_ORDER = 5;
	private static final int COL_FUND = 6;
	private static final int COL_DEPARTMENT = 7;
	private static final int COL_SCHEME = 8;
	private static final int COL_SUB_SCHEME = 9;
	private static final int COL_FUND_SOURCE = 10;
	private static final int COL_FUNCTION = 11;
	private static final int COL_NARRATION = 12;
	private static final int COL_PARTY_BILL_NO = 13;
	private static final int COL_PARTY_BILL_DATE = 14;
	private static final int COL_BILL_TYPE = 15;

	/*
	 * ========================================================= DEBIT DETAILS
	 * =========================================================
	 */

	private static final int COL_DEBIT_GL_CODE = 16;
	private static final int COL_DEBIT_ACCOUNT_HEAD = 17;
	private static final int COL_DEBIT_AMOUNT = 18;

	/*
	 * ========================================================= DEDUCTION DETAILS
	 * =========================================================
	 */

	private static final int COL_DEDUCTION_GL_CODE = 19;
	private static final int COL_DEDUCTION_ACCOUNT_HEAD = 20;
	private static final int COL_DEDUCTION_PERCENTAGE = 21;
	private static final int COL_DEDUCTION_CREDIT_AMOUNT = 22;

	/*
	 * ========================================================= NET PAYABLE DETAILS
	 * =========================================================
	 */

	private static final int COL_NET_PAYABLE_GL_CODE = 23;
	private static final int COL_NET_PAYABLE_AMOUNT = 24;

	/*
	 * Excel row 8 = Java row index 7.
	 */
	private static final int DATA_START_ROW = 7;

	/*
	 * Last required column is Y = Java index 24. Therefore expected column count =
	 * 25.
	 */
	private static final int EXPECTED_COLUMN_COUNT = 25;

	private final DataFormatter dataFormatter = new DataFormatter();

	/**
	 * Reads and validates Supplier Bill Excel file.
	 *
	 * Rows having SN belong to a new Supplier Bill. Rows without SN are
	 * continuation rows of the previous Supplier Bill.
	 */
	public List<SupplierBillRecord> read(String filePath) throws Exception {
		List<SupplierBillRecord> supplierRecords = new ArrayList<>();
		Set<Integer> serialNumbers = new HashSet<>();
		try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));Workbook workbook = WorkbookFactory.create(inputStream)) {
			
			validateWorkbook(workbook);
//			Sheet sheet = workbook.getSheetAt(0);
			Sheet sheet = workbook.getSheet("Supplier Bill");
			validateSheet(sheet);
			SupplierBillRecord currentRecord = null;

			/*
			 * ========================================================= PROCESS DATA ROWS =========================================================
			 */

			for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);

				/*
				 * Completely empty rows are allowed.
				 */
				if (isRowEmpty(row)) {
					continue;
				}

				int excelRowNumber = rowIndex + 1;

				/*
				 * Validate row structure.
				 */
				validateRowStructure(row, excelRowNumber);

				/*
				 * ===================================================== NEW BILL IDENTIFICATION =====================================================
				 *
				 * SN is the primary marker for a new Supplier Bill.
				 */

				boolean hasSn = hasValue(row, COL_SN);

				if (hasSn) {

					/*
					 * New Supplier Bill.
					 */

					Integer serialNumber = getRequiredIntegerValue(row, COL_SN, excelRowNumber, "SN");
					validateSerialNumber(serialNumber, excelRowNumber);

					/*
					 * Duplicate SN validation.
					 */

					if (!serialNumbers.add(serialNumber)) {

						throw new IllegalArgumentException("Duplicate SN '" + serialNumber + "' found at Excel row "
								+ excelRowNumber + ". Each Supplier Bill must have a unique SN.");
					}

					/*
					 * Create new Supplier Bill.
					 */

					currentRecord = createSupplierBillRecord(row, excelRowNumber);

					currentRecord.setStartRow(excelRowNumber);
					currentRecord.setEndRow(excelRowNumber);

					supplierRecords.add(currentRecord);

					log.info("New Supplier Bill detected. SN={} | StartRow={}", currentRecord.getSerialNumber(),
							currentRecord.getStartRow());

				} else {

					/*
					 * ================================================= CONTINUATION ROW
					 * =================================================
					 */

					if (currentRecord == null) {

						throw new IllegalArgumentException(
								"Invalid Excel structure at row " + excelRowNumber + ": SN is missing and no previous "
										+ "Supplier Bill exists. The first " + "data row must contain an SN.");
					}

					currentRecord.setEndRow(excelRowNumber);

					validateContinuationRow(row, excelRowNumber);
				}

				/*
				 * ===================================================== TRANSACTION DETAILS
				 * =====================================================
				 */

				addDebitDetail(row, currentRecord, excelRowNumber);

				addDeductionDetail(row, currentRecord, excelRowNumber);

				addNetPayableDetail(row, currentRecord, excelRowNumber);
			}

			/*
			 * ========================================================= FINAL RECORD
			 * VALIDATION =========================================================
			 */

			validateRecords(supplierRecords);

		} catch (IllegalArgumentException exception) {

			log.error("Supplier Bill Excel validation failed: {}", exception.getMessage());

			throw exception;

		} catch (Exception exception) {

			log.error("Unexpected error while reading Supplier Bill Excel file.", exception);

			throw new RuntimeException("Unable to read Supplier Bill Excel file '" + filePath + "'. Reason: "
					+ getExceptionMessage(exception), exception);
		}

		/*
		 * ========================================================= LOG SUMMARY
		 * =========================================================
		 */

		log.info("==============================================");
		log.info("TOTAL SUPPLIER BILL RECORDS READ: {}", supplierRecords.size());
		log.info("==============================================");

		for (SupplierBillRecord record : supplierRecords) {

			log.info(
					"SN={} | StartRow={} | EndRow={} | DebitDetails={} " + "| DeductionDetails={} | NetPayable={} "
							+ "| Supplier={} | PurchaseOrder={}",

					record.getSerialNumber(), record.getStartRow(), record.getEndRow(),

					record.getDebitDetails() != null ? record.getDebitDetails().size() : 0,

					record.getCreditDetails() != null ? record.getCreditDetails().size() : 0,

					record.getNetPayableDetails() != null ? record.getNetPayableDetails().size() : 0,

					record.getSupplier(), record.getPurchaseOrder());
		}

		log.info("==============================================");

		return supplierRecords;
	}

	/*
	 * ========================================================= VALIDATE WORKBOOK
	 * =========================================================
	 */

	private void validateWorkbook(Workbook workbook) {

		if (workbook == null) {

			throw new IllegalArgumentException("Unable to open Supplier Bill Excel workbook.");
		}

		if (workbook.getNumberOfSheets() == 0) {

			throw new IllegalArgumentException("Supplier Bill Excel workbook does not contain any worksheet.");
		}
	}

	/*
	 * ========================================================= VALIDATE SHEET
	 * =========================================================
	 */

	private void validateSheet(Sheet sheet) {

		if (sheet == null) {

			throw new IllegalArgumentException("Supplier Bill Excel worksheet is missing.");
		}

		if (sheet.getLastRowNum() < DATA_START_ROW) {

			throw new IllegalArgumentException("Supplier Bill Excel does not contain any data rows. "
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

			throw new IllegalArgumentException("Supplier Bill Excel does not contain any Supplier Bill data.");
		}
	}

	/*
	 * ========================================================= VALIDATE ROW
	 * STRUCTURE =========================================================
	 */

	private void validateRowStructure(Row row, int excelRowNumber) {

		if (row == null) {

			throw new IllegalArgumentException("Invalid empty row at Excel row " + excelRowNumber + ".");
		}

		int lastCellNumber = row.getLastCellNum();

		/*
		 * Do not reject rows only because trailing blank cells are not physically
		 * created by Excel.
		 */

		if (lastCellNumber < EXPECTED_COLUMN_COUNT) {

			log.debug("Excel row {} has {} physical columns. " + "Expected template columns: {}.", excelRowNumber,
					lastCellNumber, EXPECTED_COLUMN_COUNT);
		}
	}

	/*
	 * ========================================================= VALIDATE
	 * CONTINUATION ROW =========================================================
	 */

	private void validateContinuationRow(Row row, int excelRowNumber) {

		boolean hasDebitData = hasAnyValue(row, COL_DEBIT_GL_CODE, COL_DEBIT_ACCOUNT_HEAD, COL_DEBIT_AMOUNT);

		boolean hasDeductionData = hasAnyValue(row, COL_DEDUCTION_GL_CODE, COL_DEDUCTION_ACCOUNT_HEAD,
				COL_DEDUCTION_PERCENTAGE, COL_DEDUCTION_CREDIT_AMOUNT);

		boolean hasNetPayableData = hasAnyValue(row, COL_NET_PAYABLE_GL_CODE, COL_NET_PAYABLE_AMOUNT);

		if (!hasDebitData && !hasDeductionData && !hasNetPayableData) {

			throw new IllegalArgumentException("Invalid continuation row at Excel row " + excelRowNumber
					+ ": SN is empty and the row does not contain " + "Debit, Deduction or Net Payable details.");
		}
	}

	/*
	 * ========================================================= CREATE SUPPLIER
	 * BILL RECORD =========================================================
	 */

	private SupplierBillRecord createSupplierBillRecord(Row row, int excelRowNumber) {

		SupplierBillRecord record = new SupplierBillRecord();

		record.setSerialNumber(getRequiredIntegerValue(row, COL_SN, excelRowNumber, "SN"));

		record.setUlbName(getStringValue(row.getCell(COL_ULB_NAME)));

		record.setBillDate(getDateValue(row, COL_BILL_DATE));

		record.setSupplier(getStringValue(row.getCell(COL_SUPPLIER)));

		record.setPurchaseOrder(getStringValue(row.getCell(COL_PURCHASE_ORDER)));

		record.setFund(getStringValue(row.getCell(COL_FUND)));

		record.setDepartment(getStringValue(row.getCell(COL_DEPARTMENT)));

		record.setScheme(getStringValue(row.getCell(COL_SCHEME)));

		record.setSubScheme(getStringValue(row.getCell(COL_SUB_SCHEME)));

		record.setFundSource(getStringValue(row.getCell(COL_FUND_SOURCE)));

		record.setFunction(getStringValue(row.getCell(COL_FUNCTION)));

		record.setNarration(getStringValue(row.getCell(COL_NARRATION)));

		record.setPartyBillNo(getStringValue(row.getCell(COL_PARTY_BILL_NO)));

		record.setPartyBillDate(getDateValue(row, COL_PARTY_BILL_DATE));

		record.setBillType(getStringValue(row.getCell(COL_BILL_TYPE)));

		return record;
	}

	/*
	 * ========================================================= ADD DEBIT DETAIL
	 * =========================================================
	 */

	private void addDebitDetail(Row row, SupplierBillRecord supplierRecord, int excelRowNumber) {

		String glCode = getStringValue(row.getCell(COL_DEBIT_GL_CODE));

		String accountHead = getStringValue(row.getCell(COL_DEBIT_ACCOUNT_HEAD));

		boolean hasAmount = hasValue(row, COL_DEBIT_AMOUNT);

		BigDecimal debitAmount = getBigDecimalValue(row.getCell(COL_DEBIT_AMOUNT), excelRowNumber, "Debit Amount");

		/*
		 * Completely empty debit section.
		 */

		if (isBlank(glCode) && isBlank(accountHead) && !hasAmount) {

			return;
		}

		/*
		 * GL Code is mandatory.
		 */

		if (isBlank(glCode)) {

			throw new IllegalArgumentException(
					"Debit GL Code is missing at Excel row " + excelRowNumber + ". Debit Amount="
							+ formatValue(debitAmount) + ", Account Head=" + formatValue(accountHead) + ".");
		}

		/*
		 * Debit amount is mandatory.
		 */

		if (debitAmount == null) {

			throw new IllegalArgumentException(
					"Debit Amount is missing at Excel row " + excelRowNumber + " for Debit GL Code '" + glCode + "'.");
		}

		validatePositiveAmount(debitAmount, "Debit Amount", excelRowNumber, glCode);

		EgBillDetails detail = new EgBillDetails();

		detail.setGlcodeid(parseBigDecimalStrict(glCode, excelRowNumber, "Debit GL Code"));

		detail.setDebitamount(debitAmount);

		supplierRecord.getDebitDetails().add(detail);
	}

	/*
	 * ========================================================= ADD DEDUCTION
	 * DETAIL =========================================================
	 */

	private void addDeductionDetail(Row row, SupplierBillRecord supplierRecord, int excelRowNumber) {

		String glCode = getStringValue(row.getCell(COL_DEDUCTION_GL_CODE));

		String accountHead = getStringValue(row.getCell(COL_DEDUCTION_ACCOUNT_HEAD));

		boolean hasPercentage = hasValue(row, COL_DEDUCTION_PERCENTAGE);

		boolean hasCreditAmount = hasValue(row, COL_DEDUCTION_CREDIT_AMOUNT);

		BigDecimal percentage = getBigDecimalValue(row.getCell(COL_DEDUCTION_PERCENTAGE), excelRowNumber,
				"Deduction Percentage");

		BigDecimal creditAmount = getBigDecimalValue(row.getCell(COL_DEDUCTION_CREDIT_AMOUNT), excelRowNumber,
				"Deduction Credit Amount");

		/*
		 * Completely empty deduction section.
		 */

		if (isBlank(glCode) && isBlank(accountHead) && !hasPercentage && !hasCreditAmount) {

			return;
		}

		if (isBlank(glCode)) {

			throw new IllegalArgumentException("Deduction GL Code is missing at Excel row " + excelRowNumber + ".");
		}

		if (creditAmount == null) {

			throw new IllegalArgumentException("Deduction Credit Amount is missing at Excel row " + excelRowNumber
					+ " for Deduction GL Code '" + glCode + "'.");
		}

		validatePositiveAmount(creditAmount, "Deduction Credit Amount", excelRowNumber, glCode);

		/*
		 * Percentage is optional, but if supplied it must be 0-100.
		 */

		if (percentage != null) {

			if (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(new BigDecimal("100")) > 0) {

				throw new IllegalArgumentException("Invalid Deduction Percentage '" + percentage + "' at Excel row "
						+ excelRowNumber + " for GL Code '" + glCode + "'. Percentage must be between 0 and 100.");
			}
		}

		EgBillDetails detail = new EgBillDetails();

		detail.setGlcodeid(parseBigDecimalStrict(glCode, excelRowNumber, "Deduction GL Code"));

		detail.setCreditamount(creditAmount);

		supplierRecord.getCreditDetails().add(detail);
	}

	/*
	 * ========================================================= ADD NET PAYABLE
	 * DETAIL =========================================================
	 */

	private void addNetPayableDetail(Row row, SupplierBillRecord supplierRecord, int excelRowNumber) {

		String glCode = getStringValue(row.getCell(COL_NET_PAYABLE_GL_CODE));

		boolean hasCreditAmount = hasValue(row, COL_NET_PAYABLE_AMOUNT);

		BigDecimal creditAmount = getBigDecimalValue(row.getCell(COL_NET_PAYABLE_AMOUNT), excelRowNumber,
				"Net Payable Credit Amount");

		/*
		 * Completely empty net payable section.
		 */

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

		/*
		 * Only one Net Payable is allowed per Supplier Bill.
		 */

		if (supplierRecord.getNetPayableDetails() != null && !supplierRecord.getNetPayableDetails().isEmpty()) {

			throw new IllegalArgumentException("Multiple Net Payable details found for Supplier Bill SN "
					+ supplierRecord.getSerialNumber() + ". Another Net Payable was found at Excel row "
					+ excelRowNumber + ". Only one Net Payable detail is allowed.");
		}

		EgBillDetails detail = new EgBillDetails();

		detail.setGlcodeid(parseBigDecimalStrict(glCode, excelRowNumber, "Net Payable GL Code"));

		detail.setCreditamount(creditAmount);

		supplierRecord.getNetPayableDetails().add(detail);
	}

	/*
	 * ========================================================= FINAL RECORD
	 * VALIDATION =========================================================
	 */

	private void validateRecords(List<SupplierBillRecord> records) {

		if (records == null || records.isEmpty()) {

			throw new IllegalArgumentException("No valid Supplier Bill records were found in the Excel file.");
		}

		for (SupplierBillRecord record : records) {

			if (record == null) {

				throw new IllegalArgumentException("Excel processing produced a null Supplier Bill record.");
			}

			Integer sn = record.getSerialNumber();

			if (sn == null || sn <= 0) {

				throw new IllegalArgumentException("Invalid SN for Supplier Bill at Excel rows " + record.getStartRow()
						+ "-" + record.getEndRow() + ". SN must be greater than zero.");
			}

			if (record.getStartRow() <= 0 || record.getEndRow() < record.getStartRow()) {

				throw new IllegalArgumentException("Invalid row range for Supplier Bill SN " + sn + ": StartRow="
						+ record.getStartRow() + ", EndRow=" + record.getEndRow() + ".");
			}

			/*
			 * Required bill header fields.
			 */

			if (isBlank(record.getUlbName())) {

				throw new IllegalArgumentException("ULB Name is missing for Supplier Bill SN " + sn + " at Excel rows "
						+ record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			if (isBlank(record.getSupplier())) {

				throw new IllegalArgumentException("Supplier is missing for Supplier Bill SN " + sn + " at Excel rows "
						+ record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			/*
			 * Purchase Order validation.
			 */

			if (isBlank(record.getPurchaseOrder())) {

				throw new IllegalArgumentException("Purchase Order is missing for Supplier Bill SN " + sn
						+ " at Excel rows " + record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			/*
			 * Debit Details are mandatory.
			 */

			if (record.getDebitDetails() == null || record.getDebitDetails().isEmpty()) {

				throw new IllegalArgumentException("No Debit Details found for Supplier Bill SN " + sn
						+ " at Excel rows " + record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			/*
			 * Net Payable is mandatory.
			 */

			if (record.getNetPayableDetails() == null || record.getNetPayableDetails().isEmpty()) {

				throw new IllegalArgumentException("Net Payable Detail is missing for Supplier Bill SN " + sn
						+ " at Excel rows " + record.getStartRow() + "-" + record.getEndRow() + ".");
			}

			/*
			 * Only one Net Payable is allowed.
			 */

			if (record.getNetPayableDetails().size() > 1) {

				throw new IllegalArgumentException("Multiple Net Payable Details found for Supplier Bill SN " + sn
						+ ". Only one Net Payable detail is allowed.");
			}

			validateRecordDetails(record);
		}
	}

	/*
	 * ========================================================= VALIDATE INDIVIDUAL
	 * RECORD DETAILS =========================================================
	 */

	private void validateRecordDetails(SupplierBillRecord record) {

		int sn = record.getSerialNumber();

		/*
		 * ===================================================== DEBIT DETAILS
		 * =====================================================
		 */

		for (int i = 0; i < record.getDebitDetails().size(); i++) {

			EgBillDetails debit = record.getDebitDetails().get(i);

			if (debit == null) {

				throw new IllegalArgumentException(
						"Null Debit Detail found for Supplier Bill SN " + sn + " at Debit Detail index " + i + ".");
			}

			if (debit.getGlcodeid() == null) {

				throw new IllegalArgumentException(
						"Debit GL Code is missing for Supplier Bill SN " + sn + " at Debit Detail index " + i + ".");
			}

			if (debit.getDebitamount() == null) {

				throw new IllegalArgumentException(
						"Debit Amount is missing for Supplier Bill SN " + sn + " at Debit Detail index " + i + ".");
			}

			validatePositiveAmount(debit.getDebitamount(), "Debit Amount", record.getStartRow(),
					debit.getGlcodeid().toPlainString());
		}

		/*
		 * ===================================================== DEDUCTION DETAILS
		 * =====================================================
		 */

		if (record.getCreditDetails() != null) {

			for (int i = 0; i < record.getCreditDetails().size(); i++) {

				EgBillDetails deduction = record.getCreditDetails().get(i);

				if (deduction == null) {

					throw new IllegalArgumentException("Null Deduction Detail found for Supplier Bill SN " + sn
							+ " at Deduction Detail index " + i + ".");
				}

				if (deduction.getGlcodeid() == null) {

					throw new IllegalArgumentException("Deduction GL Code is missing for Supplier Bill SN " + sn
							+ " at Deduction Detail index " + i + ".");
				}

				if (deduction.getCreditamount() == null) {

					throw new IllegalArgumentException("Deduction Credit Amount is missing for Supplier Bill SN " + sn
							+ " at Deduction Detail index " + i + ".");
				}

				validatePositiveAmount(deduction.getCreditamount(), "Deduction Credit Amount", record.getStartRow(),
						deduction.getGlcodeid().toPlainString());
			}
		}

		/*
		 * ===================================================== NET PAYABLE
		 * =====================================================
		 */

		if (record.getNetPayableDetails() == null || record.getNetPayableDetails().isEmpty()) {

			throw new IllegalArgumentException("Net Payable Detail is missing for Supplier Bill SN " + sn + ".");
		}

		EgBillDetails netPayable = record.getNetPayableDetails().get(0);

		if (netPayable == null) {

			throw new IllegalArgumentException("Null Net Payable Detail found for Supplier Bill SN " + sn + ".");
		}

		if (netPayable.getGlcodeid() == null) {

			throw new IllegalArgumentException("Net Payable GL Code is missing for Supplier Bill SN " + sn + ".");
		}

		if (netPayable.getCreditamount() == null) {

			throw new IllegalArgumentException("Net Payable Credit Amount is missing for Supplier Bill SN " + sn + ".");
		}

		validatePositiveAmount(netPayable.getCreditamount(), "Net Payable Credit Amount", record.getStartRow(),
				netPayable.getGlcodeid().toPlainString());
	}

	/*
	 * ========================================================= REQUIRED INTEGER
	 * =========================================================
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

			throw new IllegalArgumentException("Invalid " + fieldName + " '" + rawValue + "' at Excel row "
					+ excelRowNumber + ". Expected a whole number.", exception);
		}

		if (decimalValue.stripTrailingZeros().scale() > 0) {

			throw new IllegalArgumentException("Invalid " + fieldName + " '" + rawValue + "' at Excel row "
					+ excelRowNumber + ". " + fieldName + " must be a whole number.");
		}

		try {

			return decimalValue.intValueExact();

		} catch (ArithmeticException exception) {

			throw new IllegalArgumentException("Invalid " + fieldName + " '" + rawValue + "' at Excel row "
					+ excelRowNumber + ". Value is outside the supported integer range.", exception);
		}
	}

	/*
	 * ========================================================= BIG DECIMAL
	 * =========================================================
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

				throw new IllegalArgumentException("Invalid " + fieldName + " '" + value + "' at Excel row "
						+ excelRowNumber + ". Decimal precision is too high.");
			}

			return amount;

		} catch (NumberFormatException exception) {

			throw new IllegalArgumentException("Invalid " + fieldName + " '" + value + "' at Excel row "
					+ excelRowNumber + ". Expected a valid numeric value.", exception);
		}
	}

	/*
	 * ========================================================= STRICT GL CODE
	 * PARSING =========================================================
	 *
	 * Important: Your EgBillDetails.glcodeid is BigDecimal.
	 *
	 * Therefore invalid GL code should NOT silently become null.
	 */

	private BigDecimal parseBigDecimalStrict(String value, int excelRowNumber, String fieldName) {

		if (isBlank(value)) {

			throw new IllegalArgumentException(fieldName + " is missing at Excel row " + excelRowNumber + ".");
		}

		try {

			return new BigDecimal(value.replace(",", "").trim());

		} catch (NumberFormatException exception) {

			throw new IllegalArgumentException("Invalid " + fieldName + " '" + value + "' at Excel row "
					+ excelRowNumber + ". Expected a numeric GL Code.", exception);
		}
	}

	/*
	 * ========================================================= POSITIVE AMOUNT
	 * VALIDATION =========================================================
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

	/*
	 * ========================================================= SERIAL NUMBER
	 * VALIDATION =========================================================
	 */

	private void validateSerialNumber(Integer serialNumber, int excelRowNumber) {

		if (serialNumber == null) {

			throw new IllegalArgumentException("SN is missing at Excel row " + excelRowNumber + ".");
		}

		if (serialNumber <= 0) {

			throw new IllegalArgumentException("Invalid SN '" + serialNumber + "' at Excel row " + excelRowNumber
					+ ". SN must be greater than zero.");
		}
	}

	/*
	 * ========================================================= DATE VALUE
	 * =========================================================
	 */

	private String getDateValue(Row row, int columnIndex) {

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return null;
		}

		/*
		 * Excel date cell.
		 */

		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {

			return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
		}

		String value = dataFormatter.formatCellValue(cell).trim();

		if (value.isEmpty()) {
			return null;
		}

		return normalizeDate(value);
	}

	/*
	 * ========================================================= NORMALIZE DATE
	 * =========================================================
	 */

	private String normalizeDate(String value) {

		String[] supportedFormats = { "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "MM/dd/yyyy" };

		for (String format : supportedFormats) {

			try {

				SimpleDateFormat input = new SimpleDateFormat(format);

				input.setLenient(false);

				return new SimpleDateFormat("yyyy-MM-dd").format(input.parse(value));

			} catch (Exception ignored) {

				/*
				 * Try next format.
				 */
			}
		}

		/*
		 * Keep invalid date so that validation can report it.
		 */

		return value;
	}

	/*
	 * ========================================================= HAS ANY VALUE
	 * =========================================================
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

	/*
	 * ========================================================= HAS VALUE
	 * =========================================================
	 */

	private boolean hasValue(Row row, int columnIndex) {

		if (row == null) {
			return false;
		}

		String value = getStringValue(row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL));

		return !isBlank(value);
	}

	/*
	 * ========================================================= EMPTY ROW
	 * =========================================================
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

	/*
	 * ========================================================= GET STRING VALUE
	 * =========================================================
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

	/*
	 * ========================================================= FORMAT VALUE
	 * =========================================================
	 */

	private String formatValue(Object value) {

		return value == null ? "<blank>" : "'" + value + "'";
	}

	/*
	 * ========================================================= BLANK CHECK
	 * =========================================================
	 */

	private boolean isBlank(String value) {

		return value == null || value.trim().isEmpty();
	}

	/*
	 * ========================================================= EXCEPTION MESSAGE
	 * =========================================================
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