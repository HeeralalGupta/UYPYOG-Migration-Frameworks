package org.egov.finance.migration.modules.supplier.reader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.modules.supplier.dto.SupplierRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SupplierExcelReader {

    /*
     * =========================================================
     * EXCEL ROW CONFIGURATION
     * =========================================================
     *
     * Row 1 -> Supplier Details
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
     * A  = Sl. No.
     * B  = ULB Name
     * C  = Supplier Name
     * D  = Correspondence Address
     * E  = Permanent Address
     * F  = Contact Person
     * G  = Mobile Number
     * H  = Email
     * I  = Narration
     * J  = GST Number
     * K  = GST Registered State
     * L  = Bank Name
     * M  = Bank Branch
     * N  = IFSC Code
     * O  = Bank Account Number
     * P  = Supplier Type
     * Q  = Source
     * R  = Registration Number
     * S  = Status
     * T  = PAN Number
     * U  = EPF Number
     * V  = ESI Number
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
     * L -> 11
     * M -> 12
     * N -> 13
     * O -> 14
     * P -> 15
     * Q -> 16
     * R -> 17
     * S -> 18
     * T -> 19
     * U -> 20
     * V -> 21
     */

    private static final int COL_SERIAL_NUMBER = 0;
    private static final int COL_ULB_NAME = 1;
    private static final int COL_SUPPLIER_NAME = 2;
    private static final int COL_CORRESPONDENCE_ADDRESS = 3;
    private static final int COL_PERMANENT_ADDRESS = 4;
    private static final int COL_CONTACT_PERSON = 5;
    private static final int COL_MOBILE_NUMBER = 6;
    private static final int COL_EMAIL = 7;
    private static final int COL_NARRATION = 8;
    private static final int COL_GST_NUMBER = 9;
    private static final int COL_GST_REGISTERED_STATE = 10;
    private static final int COL_BANK_NAME = 11;
    private static final int COL_BANK_BRANCH = 12;
    private static final int COL_IFSC_CODE = 13;
    private static final int COL_BANK_ACCOUNT_NUMBER = 14;
    private static final int COL_SUPPLIER_TYPE = 15;
    private static final int COL_SOURCE = 16;
    private static final int COL_REGISTRATION_NUMBER = 17;
    private static final int COL_STATUS = 18;
    private static final int COL_PAN_NUMBER = 19;
    private static final int COL_EPF_NUMBER = 20;
    private static final int COL_ESI_NUMBER = 21;

    private final DataFormatter formatter = new DataFormatter();

    /**
     * Read Supplier Excel file.
     */
    public List<SupplierRecord> read(MultipartFile file) {

        List<SupplierRecord> records = new ArrayList<>();

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
                 * Create Supplier record.
                 */
                SupplierRecord record =
                        createSupplierRecord(row);

                record.setStartRow(excelRowNumber);
                record.setEndRow(excelRowNumber);

                records.add(record);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to read Supplier Excel file.", e);
        }

        return records;
    }

    /*
     * =========================================================
     * CREATE SUPPLIER RECORD
     * =========================================================
     */
    private SupplierRecord createSupplierRecord(Row row) {

        SupplierRecord record = new SupplierRecord();

        /*
         * Sl. No.
         */
        record.setSerialNumber(
                parseInteger(
                        getCellValue(
                                row,
                                COL_SERIAL_NUMBER)));

        /*
         * ULB Name
         */
        record.setUlbName(
                getCellValue(
                        row,
                        COL_ULB_NAME));

        /*
         * Supplier Name
         */
        record.setName(
                getCellValue(
                        row,
                        COL_SUPPLIER_NAME));

        /*
         * Correspondence Address
         */
        record.setCorrespondenceAddress(
                getCellValue(
                        row,
                        COL_CORRESPONDENCE_ADDRESS));

        /*
         * Permanent Address
         *
         * Supplier entity uses paymentAddress,
         * therefore map Permanent Address to paymentAddress.
         */
        record.setPaymentAddress(
                getCellValue(
                        row,
                        COL_PERMANENT_ADDRESS));

        /*
         * Contact Person
         */
        record.setContactPerson(
                getCellValue(
                        row,
                        COL_CONTACT_PERSON));

        /*
         * Mobile Number
         */
        record.setMobileNumber(
                getCellValue(
                        row,
                        COL_MOBILE_NUMBER));

        /*
         * Email
         */
        record.setEmail(
                getCellValue(
                        row,
                        COL_EMAIL));

        /*
         * Narration
         */
        record.setNarration(
                getCellValue(
                        row,
                        COL_NARRATION));

        /*
         * GST Number
         */
        record.setTinNumber(
                getCellValue(
                        row,
                        COL_GST_NUMBER));

        /*
         * GST Registered State
         */
        record.setGstRegisteredState(
                getCellValue(
                        row,
                        COL_GST_REGISTERED_STATE));

        /*
         * Bank Name
         */
        record.setBankName(
                getCellValue(
                        row,
                        COL_BANK_NAME));

        /*
         * Bank Branch
         */
        record.setBranchName(
                getCellValue(
                        row,
                        COL_BANK_BRANCH));

        /*
         * IFSC Code
         */
        record.setIfscCode(
                getCellValue(
                        row,
                        COL_IFSC_CODE));

        /*
         * Bank Account Number
         */
        record.setBankAccount(
                getCellValue(
                        row,
                        COL_BANK_ACCOUNT_NUMBER));

        /*
         * Supplier Type
         */
        record.setSupplierType(
                getCellValue(
                        row,
                        COL_SUPPLIER_TYPE));

        /*
         * Source
         */
        record.setSource(
                getCellValue(
                        row,
                        COL_SOURCE));

        /*
         * Registration Number
         */
        record.setRegistrationNumber(
                getCellValue(
                        row,
                        COL_REGISTRATION_NUMBER));

        /*
         * Status
         *
         * Excel contains status text such as:
         *
         * Active
         *
         * Do NOT parse it as Integer here.
         * Convert status name -> status ID later.
         */
        record.setStatus(
                getCellValue(
                        row,
                        COL_STATUS));

        /*
         * PAN Number
         */
        record.setPanNumber(
                getCellValue(
                        row,
                        COL_PAN_NUMBER));

        /*
         * EPF Number
         */
        record.setEpfNumber(
                getCellValue(
                        row,
                        COL_EPF_NUMBER));

        /*
         * ESI Number
         */
        record.setEsiNumber(
                getCellValue(
                        row,
                        COL_ESI_NUMBER));

        return record;
    }

    /*
     * =========================================================
     * CELL VALUE
     * =========================================================
     */
    private String getCellValue(
            Row row,
            int columnIndex) {

        Cell cell = row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {

        case STRING:

            return cell
                    .getStringCellValue()
                    .trim();

        case NUMERIC:

            /*
             * Prevent values such as:
             *
             * 3.2549E+11
             *
             * for numeric Excel cells.
             */
            return new java.math.BigDecimal(
                    cell.getNumericCellValue())
                    .toBigInteger()
                    .toString();

        case BOOLEAN:

            return String.valueOf(
                    cell.getBooleanCellValue());

        case FORMULA:

            return formatter
                    .formatCellValue(cell)
                    .trim();

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

        if (value == null ||
            value.trim().isEmpty()) {

            return null;
        }

        try {

            return (int) Double.parseDouble(
                    value.replace(",", "")
                         .trim());

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
         * Check all Supplier columns.
         *
         * A -> Sl. No.
         * B -> ULB Name
         * C -> Supplier Name
         * D -> Correspondence Address
         * E -> Permanent Address
         * F -> Contact Person
         * G -> Mobile Number
         * H -> Email
         * I -> Narration
         * J -> GST Number
         * K -> GST Registered State
         * L -> Bank Name
         * M -> Bank Branch
         * N -> IFSC Code
         * O -> Bank Account Number
         * P -> Supplier Type
         * Q -> Source
         * R -> Registration Number
         * S -> Status
         * T -> PAN Number
         * U -> EPF Number
         * V -> ESI Number
         */

        for (int i = COL_SERIAL_NUMBER;
             i <= COL_ESI_NUMBER;
             i++) {

            if (!getCellValue(row, i).isEmpty()) {
                return false;
            }
        }

        return true;
    }
}