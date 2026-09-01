package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class BankBranchRowValidator implements MigrationRowValidator {

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
         * 2. BANK
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "bank",
                "Bank",
                validationError);

        /*
         * =====================================================
         * 3. BRANCH NAME / LOCATION
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "branchnamelocation",
                "Branch Name/Location",
                validationError);

        /*
         * =====================================================
         * 4. IFSC CODE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "ifsccode",
                "IFSC Code",
                validationError);

        /*
         * =====================================================
         * 5. BRANCH CODE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "branchcode",
                "Branch Code",
                validationError);

        /*
         * =====================================================
         * 6. ADDRESS
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "address",
                "Address",
                validationError);

        /*
         * =====================================================
         * 7. DUPLICATE BANK + BRANCH
         * =====================================================
         *
         * Duplicate means:
         *
         * Same ULB
         * +
         * Same Bank
         * +
         * Same Branch Name/Location
         *
         * Example:
         *
         * Gurugram | SBI | Sector 17
         * Gurugram | SBI | Sector 17
         *
         * Second row = duplicate.
         *
         * But:
         *
         * Gurugram | SBI | Sector 17
         * Gurugram | SBI | Sector 18
         *
         * is allowed.
         *
         * And:
         *
         * Gurugram | SBI | Sector 17
         * Faridabad | SBI | Sector 17
         *
         * is also allowed.
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
                        "bank");

        String branchName =
                getValue(
                        row,
                        headerMap,
                        "branchnamelocation");

        if (!ulbName.isEmpty()
                && !bankName.isEmpty()
                && !branchName.isEmpty()) {

            if (isDuplicateBankBranch(
                    row,
                    headerMap,
                    ulbName,
                    bankName,
                    branchName)) {

                validationError.getErrors().add(
                        "Duplicate Bank and Branch found: Bank '"
                                + bankName
                                + "', Branch '"
                                + branchName
                                + "', ULB '"
                                + ulbName
                                + "'");
            }
        }

        /*
         * =====================================================
         * RETURN
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
     * DUPLICATE BANK + BRANCH CHECK
     * =========================================================
     *
     * Checks previous Excel rows only.
     *
     * Duplicate combination:
     *
     * ULB Name
     * +
     * Bank
     * +
     * Branch Name/Location
     *
     * Comparison is case-insensitive.
     *
     * Leading/trailing spaces are ignored.
     */

    private boolean isDuplicateBankBranch(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentBankName,
            String currentBranchName) {

        int currentRowNumber =
                currentRow.getRowNum();

        /*
         * =====================================================
         * CHECK PREVIOUS ROWS
         * =====================================================
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
             * Previous ULB.
             */

            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            /*
             * Previous Bank.
             */

            String previousBankName =
                    getValue(
                            previousRow,
                            headerMap,
                            "bank");

            /*
             * Previous Branch.
             */

            String previousBranchName =
                    getValue(
                            previousRow,
                            headerMap,
                            "branchnamelocation");

            /*
             * Skip incomplete rows.
             */

            if (previousUlbName.isEmpty()
                    || previousBankName.isEmpty()
                    || previousBranchName.isEmpty()) {

                continue;
            }

            /*
             * =================================================
             * COMPARE ULB
             * =================================================
             */

            boolean sameUlb =
                    previousUlbName
                            .trim()
                            .equalsIgnoreCase(
                                    currentUlbName.trim());

            /*
             * =================================================
             * COMPARE BANK
             * =================================================
             */

            boolean sameBank =
                    previousBankName
                            .trim()
                            .equalsIgnoreCase(
                                    currentBankName.trim());

            /*
             * =================================================
             * COMPARE BRANCH
             * =================================================
             */

            boolean sameBranch =
                    previousBranchName
                            .trim()
                            .equalsIgnoreCase(
                                    currentBranchName.trim());

            /*
             * =================================================
             * ALL THREE SAME = DUPLICATE
             * =================================================
             */

            if (sameUlb
                    && sameBank
                    && sameBranch) {

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