package org.egov.finance.migration.service.validator;

import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class ContractorRowValidator implements MigrationRowValidator {

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
         * 2. CONTRACTOR NAME
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "contractorname",
                "Contractor Name",
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
         * 11. CONTRACTOR TYPE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "contractortype",
                "Contractor Type",
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
         * 15. DUPLICATE CONTRACTOR NAME
         * =====================================================
         *
         * Duplicate means:
         *
         * Same ULB + Same Contractor Name
         *
         * Example:
         *
         * Gurugram | Anand Prakesh
         * Gurugram | Anand Prakesh
         *
         * The second row will be rejected.
         *
         * Different ULB is allowed:
         *
         * Gurugram  | Anand Prakesh
         * Faridabad | Anand Prakesh
         *
         * This is NOT considered duplicate.
         */

        String ulbName =
                getValue(
                        row,
                        headerMap,
                        "ulbname");

        String contractorName =
                getValue(
                        row,
                        headerMap,
                        "contractorname");

        if (!ulbName.isEmpty()
                && !contractorName.isEmpty()) {

            if (isDuplicateContractor(
                    row,
                    headerMap,
                    ulbName,
                    contractorName)) {

                validationError.getErrors().add(
                        "Duplicate Contractor Name '"
                                + contractorName
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
     * DUPLICATE CONTRACTOR CHECK
     * =========================================================
     *
     * Checks previous Excel rows only.
     *
     * Duplicate combination:
     *
     * ULB Name + Contractor Name
     *
     * Comparison:
     * - Case insensitive
     * - Leading/trailing spaces ignored
     */

    private boolean isDuplicateContractor(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentContractorName) {

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
             * Previous ULB.
             */
            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            /*
             * Previous Contractor Name.
             */
            String previousContractorName =
                    getValue(
                            previousRow,
                            headerMap,
                            "contractorname");

            /*
             * Ignore incomplete rows.
             */
            if (previousUlbName.isEmpty()
                    || previousContractorName.isEmpty()) {

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
             * Compare Contractor Name.
             */
            boolean sameContractor =
                    previousContractorName
                            .trim()
                            .equalsIgnoreCase(
                                    currentContractorName.trim());

            /*
             * Both same => duplicate.
             */
            if (sameUlb && sameContractor) {

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