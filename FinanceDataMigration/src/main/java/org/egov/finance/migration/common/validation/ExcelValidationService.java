package org.egov.finance.migration.common.validation;


import java.io.InputStream;
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
import org.egov.finance.migration.common.dto.MigrationValidationResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExcelValidationService {

    private static final long MAX_FILE_SIZE =
            25 * 1024 * 1024;

    private static final int MAX_HEADER_SEARCH_ROWS = 20;

    private final DataFormatter formatter =
            new DataFormatter();


    public MigrationValidationResult validate(
            MultipartFile file,
            String moduleCode) {

        MigrationValidationResult result =
                new MigrationValidationResult();

        result.setModuleCode(moduleCode);


        /* =========================================
           BASIC FILE VALIDATION
        ========================================= */

        if (file == null || file.isEmpty()) {

            result.setValid(false);

            result.getErrors().add(
                    "Please select an Excel file."
            );

            return result;
        }


        result.setFileName(
                file.getOriginalFilename()
        );


        if (file.getSize() > MAX_FILE_SIZE) {

            result.setValid(false);

            result.getErrors().add(
                    "Maximum allowed file size is 25 MB."
            );

            return result;
        }


        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                !isExcelFile(fileName)) {

            result.setValid(false);

            result.getErrors().add(
                    "Only XLS and XLSX files are supported."
            );

            return result;
        }


        /* =========================================
           EXPECTED COLUMNS
        ========================================= */

        List<String> expectedColumns =
                MigrationColumnConfig
                        .getRequiredColumns(moduleCode);


        if (expectedColumns.isEmpty()) {

            result.setValid(false);

            result.getErrors().add(
                    "No column configuration found for module: "
                    + moduleCode
            );

            return result;
        }


        /* =========================================
           READ EXCEL
        ========================================= */

        try (InputStream inputStream =
                     file.getInputStream();

             Workbook workbook =
                     WorkbookFactory.create(inputStream)) {


            if (workbook.getNumberOfSheets() == 0) {

                result.setValid(false);

                result.getErrors().add(
                        "The Excel file does not contain any worksheet."
                );

                return result;
            }


            Sheet sheet =
                    workbook.getSheetAt(0);


            if (sheet.getLastRowNum() < 0) {

                result.setValid(false);

                result.getErrors().add(
                        "The Excel worksheet is empty."
                );

                return result;
            }


            /* =====================================
               FIND HEADER
            ===================================== */

            int headerRowIndex =
                    findHeaderRow(
                            sheet,
                            expectedColumns
                    );


            if (headerRowIndex == -1) {

                result.setValid(false);

                result.getErrors().add(
                        "Unable to identify the header row."
                );

                result.getErrors().add(
                        "Please make sure the Excel file "
                        + "uses the correct migration template."
                );

                return result;
            }


            /*
             * Convert POI zero-based row number
             * into normal Excel row number.
             */
            int excelHeaderRow =
                    headerRowIndex + 1;

            result.setHeaderRow(
                    excelHeaderRow
            );


            /* =====================================
               READ HEADER COLUMNS
            ===================================== */

            Row headerRow =
                    sheet.getRow(headerRowIndex);


            List<String> foundColumns =
                    readHeaderColumns(headerRow);


            result.setFoundColumns(
                    foundColumns
            );


            result.setColumnCount(
                    foundColumns.size()
            );


            /* =====================================
               CHECK REQUIRED COLUMNS
            ===================================== */

            List<String> missingColumns =
                    findMissingColumns(
                            expectedColumns,
                            foundColumns
                    );


            result.setMissingColumns(
                    missingColumns
            );


            if (!missingColumns.isEmpty()) {

                result.setValid(false);

                for (String missing :
                        missingColumns) {

                    result.getErrors().add(
                            "Missing required column: "
                            + missing
                    );
                }

                return result;
            }


            /* =====================================
               COUNT DATA ROWS
            ===================================== */

            int dataRows =
                    countDataRows(
                            sheet,
                            headerRowIndex
                    );


            result.setDataRows(
                    dataRows
            );


            if (dataRows == 0) {

                result.setValid(false);

                result.getErrors().add(
                        "No data rows were found below the header."
                );

                return result;
            }


            /* =====================================
               SUCCESS
            ===================================== */

            result.setValid(true);

            return result;


        } catch (Exception e) {

            result.setValid(false);

            result.getErrors().add(
                    "Unable to read the Excel file: "
                    + e.getMessage()
            );

            return result;
        }
    }


    /* =================================================
       FIND HEADER ROW
    ================================================= */

    private int findHeaderRow(
            Sheet sheet,
            List<String> expectedColumns) {

        int lastRow =
                sheet.getLastRowNum();


        int rowsToCheck =
                Math.min(
                        lastRow + 1,
                        MAX_HEADER_SEARCH_ROWS
                );


        for (int rowIndex = 0;
             rowIndex < rowsToCheck;
             rowIndex++) {


            Row row =
                    sheet.getRow(rowIndex);


            if (row == null) {
                continue;
            }


            int matchedColumns = 0;


            for (String expected :
                    expectedColumns) {

                boolean found = false;


                for (Cell cell : row) {

                    String actual =
                            getCellValue(cell);


                    if (normalize(actual)
                            .equals(
                                    normalize(expected)
                            )) {

                        found = true;

                        break;
                    }
                }


                if (found) {
                    matchedColumns++;
                }
            }


            /*
             * Header is accepted when at least
             * 70% of required columns match.
             */
            double matchPercentage =
                    (double) matchedColumns
                    / expectedColumns.size();


            if (matchPercentage >= 0.70) {

                return rowIndex;
            }
        }


        return -1;
    }


    /* =================================================
       READ HEADER
    ================================================= */

    private List<String> readHeaderColumns(
            Row headerRow) {

        List<String> columns =
                new ArrayList<String>();


        if (headerRow == null) {
            return columns;
        }


        for (Cell cell : headerRow) {

            String value =
                    getCellValue(cell);


            if (value != null &&
                    !value.trim().isEmpty()) {

                columns.add(
                        value.trim()
                );
            }
        }


        return columns;
    }


    /* =================================================
       FIND MISSING COLUMNS
    ================================================= */

    private List<String> findMissingColumns(
            List<String> expected,
            List<String> actual) {

        List<String> missing =
                new ArrayList<String>();


        Set<String> normalizedActual =
                new HashSet<String>();


        for (String column : actual) {

            normalizedActual.add(
                    normalize(column)
            );
        }


        for (String required : expected) {

            if (!normalizedActual.contains(
                    normalize(required)
            )) {

                missing.add(required);
            }
        }


        return missing;
    }


    /* =================================================
       COUNT DATA ROWS
    ================================================= */

    private int countDataRows(
            Sheet sheet,
            int headerRowIndex) {

        int count = 0;


        for (int i =
                headerRowIndex + 1;
                i <= sheet.getLastRowNum();
                i++) {


            Row row =
                    sheet.getRow(i);


            if (row == null) {
                continue;
            }


            boolean hasData = false;


            for (Cell cell : row) {

                String value =
                        getCellValue(cell);


                if (value != null &&
                        !value.trim().isEmpty()) {

                    hasData = true;

                    break;
                }
            }


            if (hasData) {
                count++;
            }
        }


        return count;
    }


    /* =================================================
       CELL VALUE
    ================================================= */

    private String getCellValue(
            Cell cell) {

        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell);
    }


    /* =================================================
       NORMALIZE HEADER
    ================================================= */

    private String normalize(
            String value) {

        if (value == null) {
            return "";
        }


        return value
                .trim()
                .toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }


    /* =================================================
       FILE TYPE
    ================================================= */

    private boolean isExcelFile(
            String fileName) {

        String lower =
                fileName.toLowerCase();

        return lower.endsWith(".xls")
                || lower.endsWith(".xlsx");
    }
}
