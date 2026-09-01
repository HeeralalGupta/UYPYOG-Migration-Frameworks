package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class FundRowValidator implements MigrationRowValidator {

    private final DataFormatter formatter = new DataFormatter();

    @Override
    public RowValidationError validate(
            Row row,
            int excelRowNumber,
            Map<String, Integer> headerMap) {

        RowValidationError validationError =
                new RowValidationError(excelRowNumber);

        /*
         * =====================================================
         * 1. ULB NAME
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "ulbname",
                "ULB Name",
                validationError);

        /*
         * =====================================================
         * 2. FUND NAME
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "fundname",
                "Fund Name",
                validationError);

        /*
         * =====================================================
         * 3. NATURE OF FUND
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "natureoffund",
                "Nature of Fund",
                validationError);

        /*
         * =====================================================
         * 4. DUPLICATE FUND NAME
         * =====================================================
         *
         * Same Fund Name should not be repeated
         * for the same ULB.
         *
         * Example:
         *
         * Gurugram | General Fund | MC Fund
         * Gurugram | General Fund | MC Fund
         *
         * The second row will be rejected.
         */

        String ulbName =
                getValue(row, headerMap, "ulbname");

        String fundName =
                getValue(row, headerMap, "fundname");

        if (!ulbName.isEmpty() && !fundName.isEmpty()) {

            if (isDuplicateFund(
                    row,
                    headerMap,
                    ulbName,
                    fundName)) {

                validationError.getErrors().add(
                        "Duplicate Fund Name '" + fundName
                                + "' found for ULB '" + ulbName + "'");
            }
        }

        /*
         * =====================================================
         * 5. RETURN
         * =====================================================
         */

        return validationError;
    }

    /*
     * =========================================================
     * REQUIRED FIELD VALIDATION
     * =========================================================
     */

    private void validateRequired(
            Row row,
            Map<String, Integer> headerMap,
            String header,
            String displayName,
            RowValidationError result) {

        String value =
                getValue(row, headerMap, header);

        if (value.isEmpty()) {

            result.getErrors().add(
                    displayName + " is required");
        }
    }

    /*
     * =========================================================
     * DUPLICATE FUND CHECK
     * =========================================================
     *
     * Checks previous Excel rows only.
     *
     * Duplicate means:
     *
     * Same ULB + Same Fund Name
     *
     * Nature of Fund is not considered for duplicate check.
     */

    private boolean isDuplicateFund(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentFundName) {

        int currentRowNumber =
                currentRow.getRowNum();

        /*
         * Check all previous rows.
         */
        for (int i = 0;
                i < currentRowNumber;
                i++) {

            Row previousRow =
                    currentRow.getSheet().getRow(i);

            if (previousRow == null) {
                continue;
            }

            /*
             * Get previous ULB.
             */
            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            /*
             * Get previous Fund Name.
             */
            String previousFundName =
                    getValue(
                            previousRow,
                            headerMap,
                            "fundname");

            if (previousUlbName.isEmpty()
                    || previousFundName.isEmpty()) {

                continue;
            }

            /*
             * Case-insensitive comparison.
             *
             * Also ignores leading/trailing spaces.
             */
            boolean sameUlb =
                    previousUlbName.trim()
                            .equalsIgnoreCase(
                                    currentUlbName.trim());

            boolean sameFund =
                    previousFundName.trim()
                            .equalsIgnoreCase(
                                    currentFundName.trim());

            if (sameUlb && sameFund) {
                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * GET CELL VALUE
     * =========================================================
     */

    private String getValue(
            Row row,
            Map<String, Integer> headerMap,
            String header) {

        Integer columnIndex =
                headerMap.get(header);

        if (columnIndex == null) {
            return "";
        }

        Cell cell =
                row.getCell(
                        columnIndex,
                        Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }
}