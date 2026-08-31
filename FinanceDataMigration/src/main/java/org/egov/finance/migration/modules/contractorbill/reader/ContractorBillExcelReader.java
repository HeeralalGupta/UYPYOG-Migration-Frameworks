package org.egov.finance.migration.modules.contractorbill.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.contractorbill.dto.ContractorBillRecord;
import org.egov.finance.migration.modules.contractorbill.dto.EgBillDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContractorBillExcelReader {

	/*
	 * ========================================================= EXCEL ROW
	 * CONFIGURATION =========================================================
	 *
	 * Row 1 -> Title Row 2 -> Blank Row 3 -> Group headers Row 4 -> Actual column
	 * headers Row 5 -> First data row
	 */

	private static final int HEADER_ROW_1 = 2;
	private static final int HEADER_ROW_2 = 3;

	private static final int DATA_START_ROW = 4;

	/*
	 * ========================================================= COLUMN INDEX
	 * =========================================================
	 *
	 * Excel columns:
	 *
	 * A = SN B = ULB Name C = Bill Date D = Contractor E = Work Order F = Fund G =
	 * Department H = Scheme I = Fund Source J = Function K = Narration L = Party
	 * Bill No M = Party Bill Date N = Party Bill Amount O = Bill Type P = Debit GL
	 * Code Q = Debit Account Head R = Debit Amount S = Credit/Deduction GL Code T =
	 * Credit/Deduction Account Head U = Deduction Percentage V = Credit Amount W =
	 * Net Payable GL Code X = Net Payable Credit Amount Y = Blank
	 *
	 * Java indexes start from 0.
	 */

	private static final int COL_SN = 0;

	private static final int COL_ULB_NAME = 1;
	private static final int COL_BILL_DATE = 2;
	private static final int COL_CONTRACTOR = 3;
	private static final int COL_WORK_ORDER = 4;
	private static final int COL_FUND = 5;
	private static final int COL_DEPARTMENT = 6;
	private static final int COL_SCHEME = 7;
	private static final int COL_FUND_SOURCE = 8;
	private static final int COL_FUNCTION = 9;
	private static final int COL_NARRATION = 10;
	private static final int COL_PARTY_BILL_NO = 11;
	private static final int COL_PARTY_BILL_DATE = 12;
	private static final int COL_PARTY_BILL_AMOUNT = 13;
	private static final int COL_BILL_TYPE = 14;

	/*
	 * Debit details
	 */
	private static final int COL_DEBIT_GL_CODE = 15;
	private static final int COL_DEBIT_ACCOUNT_HEAD = 16;
	private static final int COL_DEBIT_AMOUNT = 17;

	/*
	 * Credit / Deduction details
	 */
	private static final int COL_CREDIT_GL_CODE = 18;
	private static final int COL_CREDIT_ACCOUNT_HEAD = 19;
	private static final int COL_DEDUCTION_PERCENTAGE = 20;
	private static final int COL_CREDIT_AMOUNT = 21;

	/*
	 * Net payable details
	 */
	private static final int COL_NET_PAYABLE_GL_CODE = 22;
	private static final int COL_NET_PAYABLE_AMOUNT = 23;

	private final DataFormatter formatter = new DataFormatter();

	/*
	 * ========================================================= READ EXCEL
	 * =========================================================
	 */

	public List<ContractorBillRecord> read(MultipartFile file) {

		List<ContractorBillRecord> records = new ArrayList<>();

		try (InputStream inputStream = file.getInputStream();
				Workbook workbook = WorkbookFactory.create(inputStream)) {
			
			Sheet sheet = workbook.getSheetAt(0);
			ContractorBillRecord currentRecord = null;

			/*
			 * ===================================================== LOOP THROUGH DATA ROWS
			 * =====================================================
			 */

			for (int rowIndex = DATA_START_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				/*
				 * Ignore null / empty rows
				 */

				if (row == null || isEmptyRow(row)) {
					continue;
				}

				/*
				 * User-visible Excel row number
				 */

				int excelRowNumber = rowIndex + 1;

				/*
				 * ================================================= CHECK FOR NEW BILL
				 * =================================================
				 *
				 * In your Excel:
				 *
				 * Row 5: ULB Name = Gurugram Municipal Corporation
				 *
				 * Row 6: ULB Name = blank
				 *
				 * Row 7: ULB Name = blank
				 *
				 * Row 8: ULB Name = blank
				 *
				 * Row 9: ULB Name = Gurugram Municipal Corporation
				 *
				 * Therefore row 5-8 = one bill and row 9 starts another bill.
				 */

				boolean newBill = isNewBillRow(row);

				if (newBill) {

					/*
					 * Save previous bill
					 */

					if (currentRecord != null) {
						currentRecord.setEndRow(excelRowNumber - 1);
						records.add(currentRecord);
					}

					/*
					 * Create new bill
					 */

					currentRecord = createBillRecord(row);
					currentRecord.setStartRow(excelRowNumber);
				}

				/*
				 * Safety check
				 */

				if (currentRecord == null) {
					continue;
				}

				/*
				 * ================================================= READ DEBIT DETAIL
				 * =================================================
				 */

				EgBillDetails debitDetail = readDebitDetail(row);

				if (debitDetail != null) {
					currentRecord.getDebitDetails().add(debitDetail);
				}

				/*
				 * ================================================= READ CREDIT / DEDUCTION
				 * DETAIL =================================================
				 */

				EgBillDetails creditDetail = readCreditDetail(row);

				if (creditDetail != null) {
					currentRecord.getCreditDetails().add(creditDetail);
				}

				/*
				 * ================================================= READ NET PAYABLE DETAIL
				 * =================================================
				 */

				EgBillDetails netPayableDetail = readNetPayableDetail(row);

				if (netPayableDetail != null) {
					currentRecord.getNetPayableDetails().add(netPayableDetail);
				}

				/*
				 * Always update end row
				 */

				currentRecord.setEndRow(excelRowNumber);
			}

			/*
			 * ===================================================== ADD LAST BILL
			 * =====================================================
			 */

			if (currentRecord != null) {
				records.add(currentRecord);
			}

		} catch (Exception e) {

			throw new RuntimeException("Unable to read Contractor Bill Excel file.", e);
		}

		return records;
	}

	/*
	 * ========================================================= DETECT NEW BILL
	 * =========================================================
	 */

	private boolean isNewBillRow(Row row) {

		/*
		 * Primary marker:
		 *
		 * ULB Name must be populated on the first row of every bill.
		 */

		return !getCellValue(row, COL_ULB_NAME).isEmpty();
	}

	/*
	 * ========================================================= CREATE BILL RECORD
	 * =========================================================
	 */

	private ContractorBillRecord createBillRecord(Row row) {

		ContractorBillRecord record = new ContractorBillRecord();

		record.setUlbName(getCellValue(row, COL_ULB_NAME));
		record.setBillDate(getDateValue(row, COL_BILL_DATE));
		record.setContractor(getCellValue(row, COL_CONTRACTOR));
		record.setWorkOrder(getCellValue(row, COL_WORK_ORDER));
		record.setFund(getCellValue(row, COL_FUND));
		record.setDepartment(getCellValue(row, COL_DEPARTMENT));
		record.setScheme(getCellValue(row, COL_SCHEME));
		record.setFundSource(getCellValue(row, COL_FUND_SOURCE));
		record.setFunction(getCellValue(row, COL_FUNCTION));
		record.setNarration(getCellValue(row, COL_NARRATION));
		record.setPartyBillNo(getCellValue(row, COL_PARTY_BILL_NO));
		record.setPartyBillDate(getDateValue(row, COL_PARTY_BILL_DATE));
		record.setPartyBillAmount(parseBigDecimal(getCellValue(row, COL_PARTY_BILL_AMOUNT)));
		record.setBillType(getCellValue(row, COL_BILL_TYPE));

		return record;
	}

	/*
	 * ========================================================= READ DEBIT DETAILS
	 * =========================================================
	 *
	 * Excel:
	 *
	 * P = GL Code Q = Account Head R = Debit Amount
	 *
	 * EgBillDetails supports:
	 *
	 * functionid glcodeid debitamount creditamount narration
	 *
	 * Account Head is NOT sent because the API DTO does not contain an accountHead
	 * property.
	 */

	private EgBillDetails readDebitDetail(Row row) {

		String glCode = getCellValue(row, COL_DEBIT_GL_CODE);
		String accountHead = getCellValue(row, COL_DEBIT_ACCOUNT_HEAD);
		String debitAmount = getCellValue(row, COL_DEBIT_AMOUNT);

		/*
		 * No debit data
		 */

		if (glCode.isEmpty() && accountHead.isEmpty() && debitAmount.isEmpty()) {
			return null;
		}

		EgBillDetails detail = new EgBillDetails();

		detail.setGlcodeid(parseBigDecimal(glCode));
		detail.setDebitamount(parseBigDecimal(debitAmount));

		return detail;
	}

	/*
	 * ========================================================= READ CREDIT /
	 * DEDUCTION DETAILS =========================================================
	 *
	 * Excel:
	 *
	 * S = GL Code T = Account Head U = Deduction Percentage V = Credit Amount
	 */

	private EgBillDetails readCreditDetail(Row row) {

		String glCode = getCellValue(row, COL_CREDIT_GL_CODE);
		String accountHead = getCellValue(row, COL_CREDIT_ACCOUNT_HEAD);
		String deductionPercentage = getCellValue(row, COL_DEDUCTION_PERCENTAGE);
		String creditAmount = getCellValue(row, COL_CREDIT_AMOUNT);

		/*
		 * No credit/deduction data
		 */

		if (glCode.isEmpty() && accountHead.isEmpty() && deductionPercentage.isEmpty() && creditAmount.isEmpty()) {
			return null;
		}

		EgBillDetails detail = new EgBillDetails();

		detail.setGlcodeid(parseBigDecimal(glCode));
		detail.setCreditamount(parseBigDecimal(creditAmount));

		return detail;
	}

	/*
	 * ========================================================= READ NET PAYABLE
	 * DETAILS =========================================================
	 *
	 * Excel:
	 *
	 * W = GL Code X = Credit Amount
	 */

	private EgBillDetails readNetPayableDetail(Row row) {

		String glCode = getCellValue(row, COL_NET_PAYABLE_GL_CODE);
		String amount = getCellValue(row, COL_NET_PAYABLE_AMOUNT);

		/*
		 * No net payable data
		 */

		if (glCode.isEmpty() && amount.isEmpty()) {
			return null;
		}

		EgBillDetails detail = new EgBillDetails();

		detail.setGlcodeid(parseBigDecimal(glCode));
		detail.setCreditamount(parseBigDecimal(amount));

		return detail;
	}

	/*
	 * ========================================================= GET CELL VALUE
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
	 * ========================================================= GET DATE
	 * =========================================================
	 *
	 * Output format:
	 *
	 * yyyy-MM-dd
	 *
	 * Example:
	 *
	 * Excel: 10/11/2026
	 *
	 * Result: 2026-11-10
	 */

	private String getDateValue(Row row, int columnIndex) {

		Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		if (cell == null) {
			return null;
		}

		/*
		 * Excel date cell
		 */

		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
			return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
		}

		/*
		 * String date
		 */

		String value = formatter.formatCellValue(cell).trim();

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

		String[] supportedFormats = {

				"dd/MM/yyyy",
				"dd-MM-yyyy",
				"yyyy-MM-dd",
				"MM/dd/yyyy" };

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
		 * Do not silently destroy the value.
		 *
		 * Validation can report the invalid date later.
		 */

		return value;
	}

	/*
	 * ========================================================= BIG DECIMAL
	 * =========================================================
	 */

	private BigDecimal parseBigDecimal(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		try {
			return new BigDecimal(value.replace(",", "").trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/*
	 * ========================================================= EMPTY ROW
	 * =========================================================
	 */

	private boolean isEmptyRow(Row row) {

		short lastCellNum = row.getLastCellNum();

		if (lastCellNum < 0) {
			return true;
		}

		for (int column = 0; column < lastCellNum; column++) {
			if (!getCellValue(row, column).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}