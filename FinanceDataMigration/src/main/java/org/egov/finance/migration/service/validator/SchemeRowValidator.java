package org.egov.finance.migration.service.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.springframework.stereotype.Component;

@Component
public class SchemeRowValidator implements MigrationRowValidator {

    private final DataFormatter formatter = new DataFormatter();

    private static final String DATE_FORMAT = "dd/MM/yyyy";

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
         * 2. SCHEME NAME
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "schemename",
                "Scheme Name",
                validationError);

        /*
         * =====================================================
         * 3. FUND
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
         * 4. STATUS
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
         * 5. START DATE
         * =====================================================
         */

        String startDate =
                getValue(
                        row,
                        headerMap,
                        "startdate");

        if (startDate.isEmpty()) {

            validationError.getErrors().add(
                    "Start Date is required");

        } else if (!isValidDate(startDate)) {

            validationError.getErrors().add(
                    "Start Date must be in dd/MM/yyyy format");
        }

        /*
         * =====================================================
         * 6. END DATE
         * =====================================================
         */

        String endDate =
                getValue(
                        row,
                        headerMap,
                        "enddate");

        if (endDate.isEmpty()) {

            validationError.getErrors().add(
                    "End Date is required");

        } else if (!isValidDate(endDate)) {

            validationError.getErrors().add(
                    "End Date must be in dd/MM/yyyy format");
        }

        /*
         * =====================================================
         * 7. START DATE / END DATE RELATION
         * =====================================================
         */

        if (isValidDate(startDate)
                && isValidDate(endDate)) {

            try {

                SimpleDateFormat sdf =
                        new SimpleDateFormat(DATE_FORMAT);

                sdf.setLenient(false);

                if (sdf.parse(endDate)
                        .before(sdf.parse(startDate))) {

                    validationError.getErrors().add(
                            "End Date cannot be before Start Date");
                }

            } catch (ParseException e) {

                // Already handled above.
            }
        }

        /*
         * =====================================================
         * 8. DUPLICATE SCHEME NAME
         * =====================================================
         *
         * Duplicate means:
         *
         * Same ULB + Same Scheme Name
         *
         * Example:
         *
         * Gurugram Municipal Corporation
         * AMRUT
         *
         * Gurugram Municipal Corporation
         * AMRUT
         *
         * The second row will be rejected.
         */

        String ulbName =
                getValue(
                        row,
                        headerMap,
                        "ulbname");

        String schemeName =
                getValue(
                        row,
                        headerMap,
                        "schemename");

        if (!ulbName.isEmpty()
                && !schemeName.isEmpty()) {

            if (isDuplicateScheme(
                    row,
                    headerMap,
                    ulbName,
                    schemeName)) {

                validationError.getErrors().add(
                        "Duplicate Scheme Name '"
                                + schemeName
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
     * DUPLICATE SCHEME CHECK
     * =========================================================
     *
     * Checks only previous Excel rows.
     *
     * Duplicate condition:
     *
     * Same ULB + Same Scheme Name
     *
     * Comparison is:
     *
     * - Case insensitive
     * - Leading/trailing spaces ignored
     */

    private boolean isDuplicateScheme(
            Row currentRow,
            Map<String, Integer> headerMap,
            String currentUlbName,
            String currentSchemeName) {

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
             * Previous ULB Name
             */
            String previousUlbName =
                    getValue(
                            previousRow,
                            headerMap,
                            "ulbname");

            /*
             * Previous Scheme Name
             */
            String previousSchemeName =
                    getValue(
                            previousRow,
                            headerMap,
                            "schemename");

            if (previousUlbName.isEmpty()
                    || previousSchemeName.isEmpty()) {

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
             * Compare Scheme Name.
             */
            boolean sameScheme =
                    previousSchemeName
                            .trim()
                            .equalsIgnoreCase(
                                    currentSchemeName.trim());

            if (sameUlb && sameScheme) {
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

    /*
     * =========================================================
     * DATE VALIDATION
     * =========================================================
     */

    private boolean isValidDate(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return false;
        }

        SimpleDateFormat sdf =
                new SimpleDateFormat(DATE_FORMAT);

        sdf.setLenient(false);

        try {

            sdf.parse(value.trim());

            return true;

        } catch (ParseException e) {

            return false;
        }
    }
}