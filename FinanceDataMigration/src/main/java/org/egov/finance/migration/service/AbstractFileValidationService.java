package org.egov.finance.migration.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.MigrationRowValidator;
import org.springframework.web.multipart.MultipartFile;

public abstract class AbstractFileValidationService {

    private final MigrationRowValidator rowValidator;

    protected AbstractFileValidationService(
            MigrationRowValidator rowValidator) {

        this.rowValidator = rowValidator;
    }

    public FileValidationResult validate(MultipartFile file) {

        FileValidationResult result =
                new FileValidationResult();

        result.setFileName(file.getOriginalFilename());
        result.setModuleCode(getModuleCode().toString());

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook =
                     WorkbookFactory.create(inputStream)) {

            /*
             * =====================================================
             * 1. GET SHEET
             * =====================================================
             */

            Sheet sheet = getSheet(workbook);

            /*
             * =====================================================
             * 2. FIND HEADER ROW
             * =====================================================
             */

            int headerRowIndex =
                    findHeaderRow(sheet);

            if (headerRowIndex == -1) {

                result.setValid(false);

                result.getErrors().add(
                        "Header row not found");

                return result;
            }

            /*
             * Excel row number starts from 1
             */
            result.setHeaderRow(
                    headerRowIndex + 1);

            result.setHeaderStartRow(
                    headerRowIndex + 1);

            result.setHeaderEndRow(
                    headerRowIndex + 1);

            /*
             * =====================================================
             * 3. CREATE HEADER MAP
             * =====================================================
             */

            Row headerRow =
                    sheet.getRow(headerRowIndex);

            Map<String, Integer> headerMap =
                    createHeaderMap(headerRow);

            result.setColumnCount(
                    headerMap.size());

            /*
             * =====================================================
             * 4. VALIDATE HEADERS
             * =====================================================
             */

            if (!validateHeaders(
                    headerMap,
                    result)) {

                result.setValid(false);

                return result;
            }

            /*
             * =====================================================
             * 5. DATA START ROW
             * =====================================================
             */

            result.setDataStartRow(
                    headerRowIndex + 2);

            /*
             * =====================================================
             * 6. VALIDATE DATA ROWS
             * =====================================================
             */

            int totalRows = 0;

            for (int rowIndex =
                    headerRowIndex + 1;
                 rowIndex <= sheet.getLastRowNum();
                 rowIndex++) {

                Row row =
                        sheet.getRow(rowIndex);

                if (row == null ||
                        isEmptyRow(row)) {

                    continue;
                }

                totalRows++;

                int excelRowNumber =
                        rowIndex + 1;

                RowValidationError rowError =
                        rowValidator.validate(
                                row,
                                excelRowNumber,
                                headerMap);

                if (!rowError.getErrors().isEmpty()) {

                    result.getRowErrors()
                            .add(rowError);
                }
            }

            result.setTotalRows(totalRows);

            /*
             * =====================================================
             * 7. FINAL VALIDATION RESULT
             * =====================================================
             */

            result.setValid(
                    result.getErrors().isEmpty()
                    && result.getRowErrors().isEmpty());

        } catch (Exception e) {

            result.setValid(false);

            result.getErrors().add(
                    "Unable to validate file: "
                    + e.getMessage());
        }

        return result;
    }

    /*
     * =====================================================
     * MODULE-SPECIFIC METHODS
     * =====================================================
     */

    protected abstract MigrationType getModuleCode();

    protected abstract Sheet getSheet(
            Workbook workbook);

    protected abstract int findHeaderRow(
            Sheet sheet);

    protected abstract boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result);

    /*
     * =====================================================
     * COMMON HEADER MAP
     * =====================================================
     */

    protected Map<String, Integer> createHeaderMap(
            Row headerRow) {

        Map<String, Integer> headerMap =
                new HashMap<>();

        if (headerRow == null) {
            return headerMap;
        }

        for (Cell cell : headerRow) {

            String header =
                    cell.toString()
                            .trim()
                            .toLowerCase()
                            .replaceAll(
                                    "[^a-z0-9]",
                                    "");

            if (header.isEmpty()) {
                continue;
            }

            headerMap.put(
                    header,
                    cell.getColumnIndex());
        }

        return headerMap;
    }

    /*
     * =====================================================
     * EMPTY ROW CHECK
     * =====================================================
     */

    protected boolean isEmptyRow(Row row) {

        for (Cell cell : row) {

            if (cell != null &&
                    !cell.toString()
                            .trim()
                            .isEmpty()) {

                return false;
            }
        }

        return true;
    }
}