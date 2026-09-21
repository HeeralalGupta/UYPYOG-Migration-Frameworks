package org.egov.finance.migration.modules.work.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
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

    private final DataFormatter formatter = new DataFormatter();

    public List<WorkRecord> read(String filePath) {

        List<WorkRecord> records = new ArrayList<>();

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {

        	Sheet sheet = null;

        	for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
        	    if (ExcelConstants.WORK_SHEET.equalsIgnoreCase(
        	            workbook.getSheetName(i))) {
        	        sheet = workbook.getSheetAt(i);
        	        break;
        	    }
        	}

            if (sheet == null) {
                throw new IllegalArgumentException(
                        "Excel sheet '" +
                        ExcelConstants.WORK_SHEET +
                        "' not found.");
            }

            /*
             * Find header row dynamically.
             */
            int headerRowIndex = findHeaderRow(sheet);

            /*
             * Build header map using the detected header row.
             */
            Map<String, Integer> headerMap =
                    buildHeaderMap(sheet.getRow(headerRowIndex));

            /*
             * Read only rows after the header row.
             */
            for (int rowIndex = headerRowIndex + 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                WorkRecord record =
                        createWorkRecord(row, headerMap);

                /*
                 * Actual Excel row number.
                 *
                 * POI rowIndex is zero-based,
                 * Excel row number is one-based.
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
     * Find the header row dynamically.
     *
     * The header row is identified by checking for
     * multiple known Work Excel headers.
     */
    private int findHeaderRow(Sheet sheet) {

        for (int rowIndex = 0;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            int matchedHeaders = 0;

            for (Cell cell : row) {

                String header =
                        normalizeHeader(
                                formatter.formatCellValue(cell));

                if (isWorkHeader(header)) {
                    matchedHeaders++;
                }
            }

            /*
             * At least 5 known headers are required
             * to identify the row as the Work header.
             */
            if (matchedHeaders >= 5) {

                return rowIndex;
            }
        }

        throw new IllegalArgumentException(
                "Work Excel header row not found.");
    }

    /**
     * Build header name -> column index mapping.
     *
     * Example:
     *
     * ULB Name       -> ulbname
     * Name of Work   -> nameofwork
     * Work Type      -> worktype
     * Estimate Value -> estimatevalue
     */
    private Map<String, Integer> buildHeaderMap(
            Row headerRow) {

        Map<String, Integer> headerMap =
                new HashMap<>();

        for (Cell cell : headerRow) {

            String header =
                    normalizeHeader(
                            formatter.formatCellValue(cell));

            if (!header.isEmpty()) {

                headerMap.put(
                        header,
                        cell.getColumnIndex());
            }
        }

        return headerMap;
    }

    /**
     * Normalize Excel header.
     *
     * Examples:
     *
     * "ULB Name"       -> "ulbname"
     * "Name of Work"   -> "nameofwork"
     * "Work Type"      -> "worktype"
     * "Estimate Value" -> "estimatevalue"
     * "Start Date"     -> "startdate"
     * "End Date"       -> "enddate"
     */
    private String normalizeHeader(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Check whether the header belongs to
     * the Work Excel structure.
     */
    private boolean isWorkHeader(String header) {

        return "ulbname".equals(header)
                || "nameofwork".equals(header)
                || "worktype".equals(header)
                || "fund".equals(header)
                || "estimatevalue".equals(header)
                || "startdate".equals(header)
                || "enddate".equals(header);
    }

    /**
     * Convert one Excel row into WorkRecord.
     *
     * Column positions are determined dynamically
     * from headerMap.
     */
    private WorkRecord createWorkRecord(
            Row row,
            Map<String, Integer> headerMap) {

        WorkRecord record = new WorkRecord();

        record.setUlbName(
                getCellValue(
                        row,
                        headerMap,
                        "ulbname"));

        record.setNameOfWork(
                getCellValue(
                        row,
                        headerMap,
                        "nameofwork"));

        record.setWorkType(
                getCellValue(
                        row,
                        headerMap,
                        "worktype"));

        record.setFund(
                getCellValue(
                        row,
                        headerMap,
                        "fund"));

        record.setEstimateValue(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "estimatevalue")));

        record.setStartDate(
                parseDate(
                        getCell(
                                row,
                                headerMap,
                                "startdate")));

        record.setEndDate(
                parseDate(
                        getCell(
                                row,
                                headerMap,
                                "enddate")));

        return record;
    }

    /**
     * Get cell using dynamic header mapping.
     */
    private Cell getCell(
            Row row,
            Map<String, Integer> headerMap,
            String header) {

        Integer columnIndex =
                headerMap.get(header);

        if (columnIndex == null) {
            return null;
        }

        return row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    /**
     * Get cell value using dynamic header mapping.
     */
    private String getCellValue(
            Row row,
            Map<String, Integer> headerMap,
            String header) {

        Cell cell =
                getCell(
                        row,
                        headerMap,
                        header);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    /**
     * Parse numeric value into BigDecimal.
     */
    private BigDecimal parseBigDecimal(
            String value) {

        if (value == null ||
            value.trim().isEmpty()) {

            return null;
        }

        try {

            return new BigDecimal(
                    value
                            .replace(",", "")
                            .trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid estimate value: " + value);
        }
    }

    /**
     * Parse Excel date.
     */
    private Date parseDate(Cell cell) {

        if (cell == null ||
            cell.getCellType() == CellType.BLANK) {

            return null;
        }

        /*
         * Excel native date cell.
         */
        if (cell.getCellType() == CellType.NUMERIC &&
            DateUtil.isCellDateFormatted(cell)) {

            return cell.getDateCellValue();
        }

        /*
         * Formula cell returning date.
         */
        if (cell.getCellType() == CellType.FORMULA &&
            DateUtil.isCellDateFormatted(cell)) {

            return cell.getDateCellValue();
        }

        /*
         * Date stored as text.
         */
        String value =
                formatter
                        .formatCellValue(cell)
                        .trim();

        if (value.isEmpty()) {
            return null;
        }

        String[] formats = {

                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "dd.MM.yyyy",

                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "yyyy.MM.dd",

                "MM/dd/yyyy",
                "MM-dd-yyyy",
                "MM.dd.yyyy",

                "dd/MM/yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",

                "dd/MM/yyyy HH:mm",
                "dd-MM-yyyy HH:mm",
                "yyyy-MM-dd HH:mm"
        };

        for (String format : formats) {

            try {

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(format);

                dateFormat.setLenient(false);

                return dateFormat.parse(value);

            } catch (ParseException ignored) {

                // Try next date format.
            }
        }

        throw new IllegalArgumentException(
                "Invalid date value: " + value);
    }

    /**
     * Check whether complete row is empty.
     */
    private boolean isEmptyRow(Row row) {

        for (Cell cell : row) {

            if (!formatter
                    .formatCellValue(cell)
                    .trim()
                    .isEmpty()) {

                return false;
            }
        }

        return true;
    }
}
