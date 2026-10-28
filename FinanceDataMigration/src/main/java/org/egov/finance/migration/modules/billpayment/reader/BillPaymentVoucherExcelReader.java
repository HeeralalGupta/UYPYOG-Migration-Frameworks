
package org.egov.finance.migration.modules.billpayment.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.billpayment.dto.BillPaymentVoucherRecord;
import org.egov.finance.migration.modules.billpayment.dto.PaymentBillRecord;
import org.springframework.stereotype.Service;

@Service
public class BillPaymentVoucherExcelReader {

	// Excel row 5 is the first data row; row 4 contains headers.
	private static final int DATA_START_ROW = 4;

	private final DataFormatter formatter = new DataFormatter();

	public List<BillPaymentVoucherRecord> read(String filePath) {

		List<BillPaymentVoucherRecord> records = new ArrayList<>();

		try (InputStream in = Files.newInputStream(Paths.get(filePath));
				Workbook workbook = WorkbookFactory.create(in)) {

			Sheet sheet = workbook.getSheet("Bill Payment Voucher");

			if (sheet == null) {
				throw new IllegalArgumentException("Excel sheet 'Bill Payment Voucher' not found.");
			}

			// Preserve insertion order of voucher groups.
			Map<String, BillPaymentVoucherRecord> groupedRecords = new LinkedHashMap<>();

			Map<String, Integer> startRows = new LinkedHashMap<>();
			Map<String, Integer> endRows = new LinkedHashMap<>();

			for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {

				Row row = sheet.getRow(i);

				// Skip blank rows and continue reading.
				if (row == null || isEmptyRow(row)) {
					continue;
				}

				int excelRow = i + 1;

				BillPaymentVoucherRecord candidate = readVoucherFields(row);
				PaymentBillRecord bill = readBill(row, excelRow);

				String groupId = candidate.getVoucherGroupId() == null ? "" : candidate.getVoucherGroupId().trim();

				if (groupId.isEmpty()) {
					throw new IllegalArgumentException("Voucher Group ID is required at Excel row " + excelRow);
				}

				BillPaymentVoucherRecord existing = groupedRecords.get(groupId);

				if (existing == null) {
					existing = candidate;
					groupedRecords.put(groupId, existing);
					startRows.put(groupId, excelRow);
				} else if (!sameVoucher(existing, candidate)) {
					throw new IllegalArgumentException("Voucher-level details do not match for Voucher Group ID '"
							+ groupId + "' at Excel row " + excelRow);
				}

				// A bill must not appear twice within the same voucher.
				boolean duplicateBill = existing.getBills().stream()
						.anyMatch(existingBill -> existingBill.getBillNumber() != null
								&& existingBill.getBillNumber().trim().equalsIgnoreCase(bill.getBillNumber().trim()));

				if (duplicateBill) {
					throw new IllegalArgumentException("Duplicate bill number '" + bill.getBillNumber()
							+ "' in Voucher Group ID '" + groupId + "' at Excel row " + excelRow);
				}

				existing.getBills().add(bill);
				endRows.put(groupId, excelRow);
			}

			records.addAll(groupedRecords.values());

			for (BillPaymentVoucherRecord record : records) {
				String groupId = record.getVoucherGroupId().trim();

				record.setStartRow(startRows.get(groupId));
				record.setEndRow(endRows.get(groupId));
			}

			if (records.isEmpty()) {
				throw new IllegalArgumentException("No payment voucher records found in Excel.");
			}

			return records;

		} catch (Exception e) {
			throw new RuntimeException("Unable to read Bill Payment Voucher Excel file: " + e.getMessage(), e);
		}
	}

	private boolean sameVoucher(BillPaymentVoucherRecord a, BillPaymentVoucherRecord b) {

		return eq(a.getUlbName(), b.getUlbName()) && eq(a.getPaymentVoucherDate(), b.getPaymentVoucherDate())
				&& eq(a.getFund(), b.getFund()) && eq(a.getFunction(), b.getFunction())
				&& eq(a.getDepartment(), b.getDepartment()) && eq(a.getPaymentMode(), b.getPaymentMode())
				&& eq(a.getBankName(), b.getBankName()) && eq(a.getBankBranchName(), b.getBankBranchName())
				&& eq(a.getIfscCode(), b.getIfscCode()) && eq(a.getBankAccountNumber(), b.getBankAccountNumber())
				&& eq(a.getDescription(), b.getDescription());
	}

	private boolean eq(String a, String b) {
		return clean(a).equalsIgnoreCase(clean(b));
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}

	private BillPaymentVoucherRecord readVoucherFields(Row row) {
		BillPaymentVoucherRecord r = new BillPaymentVoucherRecord();

		r.setVoucherGroupId(value(row, 1));
		r.setUlbName(value(row, 2));
		r.setPaymentVoucherDate(value(row, 3));
		r.setFund(value(row, 4));
		r.setFunction(value(row, 5));
		r.setDepartment(value(row, 6));
		r.setPaymentMode(value(row, 7).toUpperCase(Locale.ROOT));
		r.setBankName(value(row, 8));
		r.setBankBranchName(value(row, 9));
		r.setIfscCode(value(row, 10));
		r.setBankAccountNumber(value(row, 11));
		r.setDescription(value(row, 12));

		if (r.getVoucherGroupId().trim().isEmpty()) {
			throw new IllegalArgumentException("Voucher Group ID is required at Excel row " + (row.getRowNum() + 1));
		}

		return r;
	}

	private PaymentBillRecord readBill(Row row, int excelRow) {

		PaymentBillRecord bill = new PaymentBillRecord();

		bill.setBillNumber(value(row, 13));

		if (bill.getBillNumber().isEmpty()) {
			throw new IllegalArgumentException("Bill Number is required at Excel row " + excelRow);
		}

		String amount = value(row, 14).replace(",", "");

		try {
			BigDecimal paymentAmount = new BigDecimal(amount);

			if (paymentAmount.signum() <= 0 || paymentAmount.scale() > 2) {
				throw new IllegalArgumentException("Payment Amount must be positive and have at most "
						+ "2 decimal places at Excel row " + excelRow);
			}

			bill.setPaymentAmount(paymentAmount);

		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid Payment Amount at Excel row " + excelRow);
		}

		return bill;
	}

	private String value(Row row, int index) {

		Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

		return cell == null ? "" : formatter.formatCellValue(cell).trim();
	}

	private boolean isEmptyRow(Row row) {

		if (row.getLastCellNum() < 0) {
			return true;
		}

		for (int i = 0; i < row.getLastCellNum(); i++) {
			if (!value(row, i).isEmpty()) {
				return false;
			}
		}

		return true;
	}
}
