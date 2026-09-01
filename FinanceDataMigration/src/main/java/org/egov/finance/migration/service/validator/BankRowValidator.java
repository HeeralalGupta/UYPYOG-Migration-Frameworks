package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class BankRowValidator implements MigrationRowValidator {

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
         * 2. BANK NAME
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "bankname",
                "Bank Name",
                validationError);

        /*
         * =====================================================
         * 3. NARRATION
         * =====================================================
         */

//        validateRequired(
//                row,
//                headerMap,
//                "narration",
//                "Narration",
//                validationError);

        /*
         * =====================================================
         * 4. DUPLICATE BANK NAME
         * =====================================================
         *
         * Duplicate means:
         *
         * Same ULB + Same Bank Name
         *
         * Example:
         *
         * Gurugram | HDFC Bank
         * Gurugram | HDFC Bank
         *
         * The second row will be rejected.
         *
         * Different ULB is allowed:
         *
         * Gurugram | HDFC Bank
         * Faridabad | HDFC Bank
         *
         * This is NOT considered duplicate.
         */

        String ulbName =
                getValue(
                        row,
                        headerMap,
                        "ulbname");

        String bankName =
                getValue(
                        row,
                        headerMap,
                        "bankname");

        if (!ulbName.isEmpty()
                && !bankName.isEmpty()) {

            if (isDuplicateBank(
                    row,
                    headerMap,
                    ulbName,
                    bankName)) {

                validationError.getErrors().add(
                        "Duplicate Bank Name '"
                                + bankName
                                + "' found for ULB '"
                                + ulbName
                                + "'");
            }
        }

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
                getValue(
                        row,
                        headerMap,
                        header);

        if (value.isEmpty()) {

            result.getErrors().add(
                    displayName + " is required");
        }
    }

    /*
     * =========================================================
     * DUPLICATE BANK CHECK
     * =========================================================
     *
     * Checks all previous Excel rows.
     *
     * Comparison is:
     *
     * ULB Name + Bank Name
     *
     * Case insensitive.
     *
     * Leading/trailing spaces are ignored.
     */

    private boolean isDuplicateBank(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentBankName) {

        int currentRowNumber =
                currentRow.getRowNum();

        /*
         * Check previous rows only.
         */
        for (int i = 0;
                i < currentRowNumber;
                i++) {

            Row previousRow =
                    currentRow
                            .getSheet()
                            .getRow(i);

            if (previousRow == null) {
                continue;
            }

            /*
             * Previous ULB Name.
             */
            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            /*
             * Previous Bank Name.
             */
            String previousBankName =
                    getValue(
                            previousRow,
                            headerMap,
                            "bankname");

            if (previousUlbName.isEmpty()
                    || previousBankName.isEmpty()) {

                continue;
            }

            /*
             * Compare ULB.
             */
            boolean sameUlb =
                    previousUlbName
                            .trim()
                            .equalsIgnoreCase(
                                    currentUlbName.trim());

            /*
             * Compare Bank Name.
             */
            boolean sameBank =
                    previousBankName
                            .trim()
                            .equalsIgnoreCase(
                                    currentBankName.trim());

            /*
             * Both are same => duplicate.
             */
            if (sameUlb && sameBank) {

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
                        Row.MissingCellPolicy
                                .RETURN_BLANK_AS_NULL);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }
}