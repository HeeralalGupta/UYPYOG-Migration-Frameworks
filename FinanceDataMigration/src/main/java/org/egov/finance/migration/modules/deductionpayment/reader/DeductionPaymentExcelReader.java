package org.egov.finance.migration.modules.deductionpayment.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.deductionpayment.dto.DeductionPaymentRecord;
import org.springframework.stereotype.Service;

@Service
public class DeductionPaymentExcelReader {

    /*
     * =========================================================
     * EXCEL ROW CONFIGURATION
     * =========================================================
     * Row 1 -> Title
     * Row 2 -> Note / Blank
     * Row 3 -> Headers
     * Row 4 -> First data row (index = 3)
     */
    private static final int DATA_START_ROW = 4;

    /*
     * =========================================================
     * EXCEL COLUMN CONFIGURATION
     * =========================================================
     * Column A  (0)  -> Sl. No.
     * Column B  (1)  -> ULB Name
     * Column C  (2)  -> Reference Voucher Date
     * Column D  (3)  -> Reference Voucher Number
     * Column E  (4)  -> Reference Voucher Type
     * Column F  (5)  -> Recovery
     * Column G  (6)  -> Fund
     * Column H  (7)  -> Deduction Amount
     * Column I  (8)  -> Payment Date
     * Column J  (9)  -> Bank
     * Column K  (10) -> Branch
     * Column L  (11) -> Bank Account Number
     * Column M  (12) -> Mode Of Payment
     * Column N  (13) -> Department
     * Column O  (14) -> Function
     */
    private static final int COL_SERIAL_NUMBER          = 0;
    private static final int COL_ULB_NAME               = 1;
    private static final int COL_REF_VOUCHER_DATE       = 2;
    private static final int COL_REF_VOUCHER_NUMBER     = 3;
    private static final int COL_REF_VOUCHER_TYPE       = 4;
    private static final int COL_RECOVERY               = 5;
    private static final int COL_FUND                   = 6;
    private static final int COL_DEDUCTION_AMOUNT       = 7;
    private static final int COL_PAYMENT_DATE           = 8;
    private static final int COL_BANK                   = 9;
    private static final int COL_BRANCH                 = 10;
    private static final int COL_BANK_ACCOUNT_NUMBER    = 11;
    private static final int COL_MODE_OF_PAYMENT        = 12;
    private static final int COL_DEPARTMENT             = 13;
    private static final int COL_FUNCTION               = 14;

    private static final int MAX_COLUMNS = COL_FUNCTION;

    private final DataFormatter formatter = new DataFormatter();

    /**
     * Read Deduction Payment Excel file.
     */
    public List<DeductionPaymentRecord> read(String filePath) {

        List<DeductionPaymentRecord> records = new ArrayList<>();

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = null;

            // Search for sheet named "Deduction Payment" or "DeductionPayment"
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet currentSheet = workbook.getSheetAt(i);
                String name = currentSheet.getSheetName().replace(" ", "").trim();
                if ("DeductionPayment".equalsIgnoreCase(name)) {
                    sheet = currentSheet;
                    break;
                }
            }

            // Fallback to first sheet if only one exists
            if (sheet == null && workbook.getNumberOfSheets() == 1) {
                sheet = workbook.getSheetAt(0);
            }

            if (sheet == null) {
                throw new IllegalArgumentException("Excel sheet 'Deduction Payment' not found.");
            }

            for (int rowIndex = DATA_START_ROW - 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                int excelRowNumber = rowIndex + 1;

                DeductionPaymentRecord record = createDeductionPaymentRecord(row);
                record.setStartRow(excelRowNumber);
                record.setEndRow(excelRowNumber);

                records.add(record);
            }

        } catch (Exception e) {
            throw new RuntimeException("Unable to read Deduction Payment Excel file.", e);
        }

        return records;
    }

    /*
     * =========================================================
     * CREATE DEDUCTION PAYMENT RECORD
     * =========================================================
     */
    private DeductionPaymentRecord createDeductionPaymentRecord(Row row) {
        DeductionPaymentRecord record = new DeductionPaymentRecord();

        record.setSerialNumber(parseInteger(getCellValue(row, COL_SERIAL_NUMBER)));
        record.setUlbName(getCellValue(row, COL_ULB_NAME));
        record.setVoucherDate(parseDate(row.getCell(COL_REF_VOUCHER_DATE)));
        record.setVoucherNumber(getCellValue(row, COL_REF_VOUCHER_NUMBER));
        record.setVoucherType(getCellValue(row, COL_REF_VOUCHER_TYPE));
        record.setRecovery(getCellValue(row, COL_RECOVERY));
        record.setFund(getCellValue(row, COL_FUND));
        record.setDeductionAmount(parseBigDecimal(getCellValue(row, COL_DEDUCTION_AMOUNT)));
        record.setPaymentDate(parseDate(row.getCell(COL_PAYMENT_DATE)));
        record.setBank(getCellValue(row, COL_BANK));
        record.setBranch(getCellValue(row, COL_BRANCH));
        record.setBankAccountNumber(getCellValue(row, COL_BANK_ACCOUNT_NUMBER));
        record.setModeOfPayment(getCellValue(row, COL_MODE_OF_PAYMENT));
        record.setDepartment(getCellValue(row, COL_DEPARTMENT));
        record.setFunction(getCellValue(row, COL_FUNCTION));

        return record;
    }

    /*
     * =========================================================
     * HELPER METHODS: CELL PARSING & CONVERSION
     * =========================================================
     */
    private String getCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return "";
        }
        return formatter.formatCellValue(cell).trim();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseDate(Cell cell) {
        if (cell == null) {
            return null;
        }

        // 1. If POI detects native Excel Date
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }

        // 2. Fallback: Parse common date patterns if cell is formatted as String
        String cellValue = formatter.formatCellValue(cell).trim();
        if (cellValue.isEmpty()) {
            return null;
        }

        String[] dateFormats = {"dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd/MM/yy"};
        for (String format : dateFormats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(cellValue);
            } catch (ParseException ignored) {
                // Try next format
            }
        }

        return null;
    }

    private boolean isEmptyRow(Row row) {
        for (int i = COL_SERIAL_NUMBER; i <= MAX_COLUMNS; i++) {
            if (!getCellValue(row, i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
