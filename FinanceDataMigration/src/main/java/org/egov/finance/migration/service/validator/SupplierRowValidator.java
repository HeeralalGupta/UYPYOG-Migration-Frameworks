package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class SupplierRowValidator implements MigrationRowValidator {

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
         * 2. SUPPLIER NAME
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "suppliername",
                "Supplier Name",
                validationError);

        /*
         * =====================================================
         * 3. CORRESPONDENCE ADDRESS
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "correspondenceaddress",
                "Correspondence Address",
                validationError);

        /*
         * =====================================================
         * 4. CONTACT PERSON
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "contactperson",
                "Contact Person",
                validationError);

        /*
         * =====================================================
         * 5. MOBILE NUMBER
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "mobilenumber",
                "Mobile Number",
                validationError);

        /*
         * =====================================================
         * 6. EMAIL
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "email",
                "Email",
                validationError);

        /*
         * =====================================================
         * 7. BANK NAME
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
         * 8. BANK BRANCH
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
         * 9. IFSC CODE
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
         * 10. BANK ACCOUNT NUMBER
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "bankaccountnumber",
                "Bank Account Number",
                validationError);

        /*
         * =====================================================
         * 11. SUPPLIER TYPE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "suppliertype",
                "Supplier Type",
                validationError);

        /*
         * =====================================================
         * 12. SOURCE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "source",
                "Source",
                validationError);

        /*
         * =====================================================
         * 13. STATUS
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "status",
                "Status",
                validationError);

        /*
         * =====================================================
         * 14. PAN NUMBER
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "pannumber",
                "PAN Number",
                validationError);

        /*
         * =====================================================
         * 15. DUPLICATE SUPPLIER NAME
         * =====================================================
         *
         * Duplicate means:
         *
         * Same ULB + Same Supplier Name
         *
         * Example:
         *
         * Gurugram | Akash Sharma
         * Gurugram | Akash Sharma
         *
         * Second row will be rejected.
         *
         * Different ULB is allowed.
         */

        String ulbName =
                getValue(
                        row,
                        headerMap,
                        "ulbname");

        String supplierName =
                getValue(
                        row,
                        headerMap,
                        "suppliername");

        if (!ulbName.isEmpty()
                && !supplierName.isEmpty()) {

            if (isDuplicateSupplier(
                    row,
                    headerMap,
                    ulbName,
                    supplierName)) {

                validationError.getErrors().add(
                        "Duplicate Supplier Name '"
                                + supplierName
                                + "' found for ULB '"
                                + ulbName
                                + "'");
            }
        }

        /*
         * =====================================================
         * 16. DUPLICATE BANK ACCOUNT NUMBER
         * =====================================================
         *
         * Same ULB + Same Bank Account Number
         */

        String bankAccountNumber =
                getValue(
                        row,
                        headerMap,
                        "bankaccountnumber");

        if (!ulbName.isEmpty()
                && !bankAccountNumber.isEmpty()) {

            if (isDuplicateBankAccount(
                    row,
                    headerMap,
                    ulbName,
                    bankAccountNumber)) {

                validationError.getErrors().add(
                        "Duplicate Bank Account Number '"
                                + bankAccountNumber
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
     * DUPLICATE SUPPLIER CHECK
     * =========================================================
     *
     * Checks previous Excel rows only.
     *
     * Duplicate:
     *
     * Same ULB + Same Supplier Name
     *
     * Case-insensitive comparison.
     *
     * Leading/trailing spaces ignored.
     */

    private boolean isDuplicateSupplier(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentSupplierName) {

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

            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            String previousSupplierName =
                    getValue(
                            previousRow,
                            headerMap,
                            "suppliername");

            if (previousUlbName.isEmpty()
                    || previousSupplierName.isEmpty()) {

                continue;
            }

            boolean sameUlb =
                    previousUlbName
                            .trim()
                            .equalsIgnoreCase(
                                    currentUlbName.trim());

            boolean sameSupplier =
                    previousSupplierName
                            .trim()
                            .equalsIgnoreCase(
                                    currentSupplierName.trim());

            if (sameUlb && sameSupplier) {
                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * DUPLICATE BANK ACCOUNT CHECK
     * =========================================================
     *
     * Duplicate:
     *
     * Same ULB + Same Bank Account Number
     */

    private boolean isDuplicateBankAccount(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentBankAccountNumber) {

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

            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            String previousBankAccountNumber =
                    getValue(
                            previousRow,
                            headerMap,
                            "bankaccountnumber");

            if (previousUlbName.isEmpty()
                    || previousBankAccountNumber.isEmpty()) {

                continue;
            }

            boolean sameUlb =
                    previousUlbName
                            .trim()
                            .equalsIgnoreCase(
                                    currentUlbName.trim());

            boolean sameAccount =
                    previousBankAccountNumber
                            .trim()
                            .equalsIgnoreCase(
                                    currentBankAccountNumber.trim());

            if (sameUlb && sameAccount) {
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