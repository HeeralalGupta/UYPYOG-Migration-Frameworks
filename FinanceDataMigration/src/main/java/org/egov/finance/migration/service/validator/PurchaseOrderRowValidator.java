package org.egov.finance.migration.service.validator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

import org.egov.finance.migration.common.dto.RowValidationError;

@Component
public class PurchaseOrderRowValidator implements MigrationRowValidator {

    private final DataFormatter formatter = new DataFormatter();

    private static final String DATE_FORMAT = "dd/MM/yyyy";

    @Override
    public RowValidationError validate(
            Row row,
            int excelRowNumber,
            Map<String, Integer> headerMap) {

        /*
         * This validator can be used for both
         * Purchase Order Master and Purchase Order Items.
         *
         * The sheet type is determined from
         * the first header available in the header map.
         */

        if (headerMap.containsKey("orderdate")) {
            return validatePurchaseOrderMaster(
                    row,
                    excelRowNumber,
                    headerMap);
        }

        if (headerMap.containsKey("itemdescription")) {
            return validatePurchaseOrderItems(
                    row,
                    excelRowNumber,
                    headerMap);
        }

        return new RowValidationError(excelRowNumber);
    }

    /**
     * =====================================================
     * PURCHASE ORDER MASTER VALIDATION
     * =====================================================
     */
    private RowValidationError validatePurchaseOrderMaster(
            Row row,
            int excelRowNumber,
            Map<String, Integer> headerMap) {

        RowValidationError validationError =
                new RowValidationError(excelRowNumber);

        /*
         * =================================================
         * 1. ULB NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "ulbname",
                "ULB Name",
                validationError);

        /*
         * =================================================
         * 2. ORDER NO.
         * =================================================
         *
         * Order No. is NOT mandatory in Excel,
         * so only validate if provided.
         */
        validateOptional(
                row,
                headerMap,
                "orderno",
                "Order No.",
                validationError);

        /*
         * =================================================
         * 3. ORDER DATE
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "orderdate",
                "Order Date",
                validationError);

        String orderDate =
                getValue(
                        row,
                        headerMap,
                        "orderdate");

        if (!orderDate.isEmpty()
                && !isValidDate(orderDate)) {

            validationError.getErrors().add(
                    "Order Date must be in "
                            + DATE_FORMAT
                            + " format");
        }

        /*
         * =================================================
         * 4. ORDER NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "ordername",
                "Order Name",
                validationError);

        /*
         * =================================================
         * 5. DESCRIPTION
         * =================================================
         *
         * Optional
         */

        /*
         * =================================================
         * 6. SUPPLIER NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "suppliername",
                "Supplier Name",
                validationError);

        /*
         * =================================================
         * 7. SOURCE OF FUND
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "sourceoffund",
                "Source of Fund",
                validationError);

        /*
         * =================================================
         * 8. DEPARTMENT
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "department",
                "Department",
                validationError);

        /*
         * =================================================
         * 9. SCHEME
         * =================================================
         *
         * Optional
         */

        /*
         * =================================================
         * 10. SUB SCHEME
         * =================================================
         *
         * Optional
         */

        /*
         * =================================================
         * 11. SANCTION NO.
         * =================================================
         *
         * Optional
         */

        /*
         * =================================================
         * 12. SANCTION DATE
         * =================================================
         *
         * Optional, but if provided must be valid.
         */
        String sanctionDate =
                getValue(
                        row,
                        headerMap,
                        "sanctiondate");

        if (!sanctionDate.isEmpty()
                && !isValidDate(sanctionDate)) {

            validationError.getErrors().add(
                    "Sanction Date must be in "
                            + DATE_FORMAT
                            + " format");
        }

        /*
         * =================================================
         * 13. ADVANCE PAYABLE
         * =================================================
         *
         * Optional
         */
        validateNumeric(
                row,
                headerMap,
                "advancepayable",
                "Advance Payable",
                validationError,
                false);

        /*
         * =================================================
         * 14. TOTAL ORDER VALUE
         * =================================================
         *
         * Optional
         */
        validateNumeric(
                row,
                headerMap,
                "totalordervalue",
                "Total Order Value",
                validationError,
                false);

        return validationError;
    }

    /**
     * =====================================================
     * PURCHASE ORDER ITEMS VALIDATION
     * =====================================================
     */
    private RowValidationError validatePurchaseOrderItems(
            Row row,
            int excelRowNumber,
            Map<String, Integer> headerMap) {

        RowValidationError validationError =
                new RowValidationError(excelRowNumber);

        /*
         * =================================================
         * 1. ULB NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "ulbname",
                "ULB Name",
                validationError);

        /*
         * =================================================
         * 2. ORDER NO.
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "orderno",
                "Order No.",
                validationError);

        /*
         * =================================================
         * 3. ITEM DESCRIPTION
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "itemdescription",
                "Item Description",
                validationError);

        /*
         * =================================================
         * 4. UNIT
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "unit",
                "Unit",
                validationError);

        /*
         * =================================================
         * 5. RATE
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "rate",
                "Rate",
                validationError,
                true);

        /*
         * =================================================
         * 6. GST %
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "gst",
                "GST %",
                validationError,
                true);

        /*
         * =================================================
         * 7. UNIT VALUE WITH GST
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "unitvaluewithgst",
                "Unit Value With GST",
                validationError,
                true);

        /*
         * =================================================
         * 8. QUANTITY
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "qty",
                "Qty",
                validationError,
                true);

        /*
         * =================================================
         * 9. NET AMOUNT
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "netamount",
                "Net Amount",
                validationError,
                true);

        return validationError;
    }

    /**
     * Validate required field.
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

    /**
     * Validate optional field.
     *
     * Only validates the field when a value is present.
     */
    private void validateOptional(
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
            return;
        }
    }

    /**
     * Validate numeric field.
     *
     * required = true means empty value is also invalid.
     */
    private void validateNumeric(
            Row row,
            Map<String, Integer> headerMap,
            String header,
            String displayName,
            RowValidationError result,
            boolean required) {

        String value =
                getValue(
                        row,
                        headerMap,
                        header);

        if (value.isEmpty()) {

            if (required) {

                result.getErrors().add(
                        displayName + " is required");
            }

            return;
        }

        if (!isNumeric(value)) {

            result.getErrors().add(
                    displayName + " must be numeric");

            return;
        }

        try {

            double number =
                    Double.parseDouble(
                            value
                                    .replace(",", "")
                                    .trim());

            if (number < 0) {

                result.getErrors().add(
                        displayName
                                + " cannot be negative");
            }

        } catch (NumberFormatException e) {

            result.getErrors().add(
                    "Invalid " + displayName);
        }
    }

    /**
     * Get cell value using header map.
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
                row.getCell(columnIndex);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    /**
     * Check numeric value.
     */
    private boolean isNumeric(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return false;
        }

        try {

            Double.parseDouble(
                    value
                            .replace(",", "")
                            .trim());

            return true;

        } catch (NumberFormatException e) {

            return false;
        }
    }

    /**
     * Validate date.
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

            sdf.parse(value);

            return true;

        } catch (ParseException e) {

            return false;
        }
    }
}