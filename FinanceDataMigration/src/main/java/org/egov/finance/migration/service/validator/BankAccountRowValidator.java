package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class BankAccountRowValidator implements MigrationRowValidator {

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
         * 2. BANK BRANCH
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "bankbranch",
                "Bank Branch",
                validationError);

        /*
         * =====================================================
         * 3. IFSC CODE
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
         * 4. ACCOUNT NUMBER
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "accountnumber",
                "Account Number",
                validationError);

        /*
         * =====================================================
         * 5. FUND
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "fund",
                "Fund",
                validationError);

        /*
         * =====================================================
         * 6. ACCOUNT TYPE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "accounttype",
                "Account Type",
                validationError);

        /*
         * =====================================================
         * 7. USAGE TYPE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "usagetype",
                "Usage Type",
                validationError);

        /*
         * =====================================================
         * 8. DUPLICATE ACCOUNT NUMBER
         * =====================================================
         *
         * Duplicate means:
         *
         * Same ULB + Same Account Number
         *
         * Example:
         *
         * Gurugram | 325489761245
         * Gurugram | 325489761245
         *
         * The second row will be rejected.
         *
         * Different ULB is allowed:
         *
         * Gurugram  | 325489761245
         * Faridabad | 325489761245
         *
         * This is NOT considered duplicate.
         */

        String ulbName =
                getValue(
                        row,
                        headerMap,
                        "ulbname");

        String accountNumber =
                getValue(
                        row,
                        headerMap,
                        "accountnumber");

        if (!ulbName.isEmpty()
                && !accountNumber.isEmpty()) {

            if (isDuplicateAccountNumber(
                    row,
                    headerMap,
                    ulbName,
                    accountNumber)) {

                validationError.getErrors().add(
                        "Duplicate Account Number '"
                                + accountNumber
                                + "' found for ULB '"
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
     * DUPLICATE ACCOUNT NUMBER CHECK
     * =========================================================
     *
     * Checks all previous Excel rows.
     *
     * Duplicate combination:
     *
     * ULB Name + Account Number
     *
     * Comparison:
     * - Case insensitive
     * - Leading/trailing spaces ignored
     */

    private boolean isDuplicateAccountNumber(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentAccountNumber) {

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
             * Previous Account Number.
             */
            String previousAccountNumber =
                    getValue(
                            previousRow,
                            headerMap,
                            "accountnumber");

            /*
             * Ignore incomplete previous rows.
             */
            if (previousUlbName.isEmpty()
                    || previousAccountNumber.isEmpty()) {

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
             * Compare Account Number.
             *
             * Account numbers are normally numeric,
             * but String comparison is safer because
             * Excel may contain leading zeros.
             */
            boolean sameAccountNumber =
                    previousAccountNumber
                            .trim()
                            .equalsIgnoreCase(
                                    currentAccountNumber.trim());

            /*
             * Both same => duplicate.
             */
            if (sameUlb && sameAccountNumber) {

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

        Integer columnIndex = headerMap.get(header);

        if (columnIndex == null) {
            return "";
        }

        Cell cell = row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {

        case STRING:
            return cell.getStringCellValue().trim();

        case NUMERIC:

            /*
             * Excel may store Account Number as:
             *
             * 325490000000
             *
             * but display/read it as:
             *
             * 3.2549E+11
             *
             * Convert it to a normal number string.
             */
            return new java.math.BigDecimal(
                    cell.getNumericCellValue())
                    .toBigInteger()
                    .toString();

        case BOOLEAN:
            return String.valueOf(
                    cell.getBooleanCellValue());

        case FORMULA:

            /*
             * If formula result is numeric,
             * convert it to normal number format.
             */
            if (cell.getCachedFormulaResultType()
                    == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

                return new java.math.BigDecimal(
                        cell.getNumericCellValue())
                        .toBigInteger()
                        .toString();
            }

            return formatter
                    .formatCellValue(cell)
                    .trim();

        default:
            return "";
        }
    }
}