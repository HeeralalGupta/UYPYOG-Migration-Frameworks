package org.egov.finance.migration.modules.work.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.modules.work.dto.WorkRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkExcelReader {

    private static final int DATA_START_ROW = 3;

    private final DataFormatter formatter = new DataFormatter();

    public List<WorkRecord> read(MultipartFile file) {

        List<WorkRecord> records = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheet(ExcelConstants.WORK_SHEET);

            if (sheet == null) {
                throw new IllegalArgumentException(
                        "Excel sheet '" + ExcelConstants.WORK_SHEET + "' not found.");
            }

            for (int rowIndex = DATA_START_ROW;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                WorkRecord record = createWorkRecord(row);

                /*
                 * Actual Excel row number
                 */
                record.setRowNumber(rowIndex + 1);

                records.add(record);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to read Work Excel file.", e);
        }

        return records;
    }

    /**
     * Convert one Excel row into WorkRecord.
     */
    private WorkRecord createWorkRecord(Row row) {

        WorkRecord record = new WorkRecord();

        /*
         * Excel columns:
         *
         * A -> Sl. No.
         * B -> ULB Name
         * C -> Name of Work
         * D -> Work Type
         * E -> Fund
         * F -> Estimate Value
         * G -> Start Date
         * H -> End Date
         */

        record.setUlbName(getCellValue(row, 1));
        record.setNameOfWork(getCellValue(row, 2));
        record.setWorkType(getCellValue(row, 3));
        record.setFund(getCellValue(row, 4));

        record.setEstimateValue(
                parseBigDecimal(getCellValue(row, 5)));

        record.setStartDate(
                parseDate(row.getCell(6)));

        record.setEndDate(
                parseDate(row.getCell(7)));

        return record;
    }

    /**
     * Read cell value as String.
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

    /**
     * Parse numeric value into BigDecimal.
     */
    private BigDecimal parseBigDecimal(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {

            return new BigDecimal(
                    value.replace(",", "").trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid estimate value: " + value);
        }
    }

    /**
     * Parse Excel date into java.util.Date.
     */
    private Date parseDate(Cell cell) {

        if (cell == null) {
            return null;
        }

        /*
         * Excel native date cell
         */
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue();
        }

        /*
         * Date stored as text
         */
        String value = formatter.formatCellValue(cell).trim();

        if (value.isEmpty()) {
            return null;
        }

        String[] formats = {
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy"
        };

        for (String format : formats) {

            try {

                java.text.SimpleDateFormat dateFormat =
                        new java.text.SimpleDateFormat(format);

                dateFormat.setLenient(false);

                return dateFormat.parse(value);

            } catch (java.text.ParseException ignored) {
                // Try next format
            }
        }

        throw new IllegalArgumentException(
                "Invalid date value: " + value);
    }

    /**
     * Check whether the complete row is empty.
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