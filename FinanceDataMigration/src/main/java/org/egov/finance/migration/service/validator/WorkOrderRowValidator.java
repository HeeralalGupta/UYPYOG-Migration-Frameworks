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
public class WorkOrderRowValidator implements MigrationRowValidator {

    private final DataFormatter formatter = new DataFormatter();

    private static final String DATE_FORMAT = "dd/MM/yyyy";


    @Override
    public RowValidationError validate(
            Row row,
            int excelRowNumber,
            Map<String, Integer> headerMap) {

        /*
         * This validator can be used for both
         * Work Order Master and Work Order Items.
         *
         * The sheet name is determined from the
         * first header available in the header map.
         */

        if (headerMap.containsKey("ulbname")) {

            return validateWorkOrderMaster(
                    row,
                    excelRowNumber,
                    headerMap);

        }

        if (headerMap.containsKey("itemname")) {

            return validateWorkOrderItems(
                    row,
                    excelRowNumber,
                    headerMap);
        }

        return new RowValidationError(excelRowNumber);
    }

    /**
     * =====================================================
     * WORK ORDER MASTER VALIDATION
     * =====================================================
     */
    private RowValidationError validateWorkOrderMaster(
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
         * 2. TENDER NUMBER
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "tendernumber",
                "Tender Number",
                validationError);

        /*
         * =================================================
         * 3. WORK ORDER NO.
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workorderno",
                "Work Order No.",
                validationError);

        /*
         * =================================================
         * 4. WORK ORDER DATE
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workorderdate",
                "Work Order Date",
                validationError);

        String workOrderDate =
                getValue(
                        row,
                        headerMap,
                        "workorderdate");

        if (!workOrderDate.isEmpty()
                && !isValidDate(workOrderDate)) {

            validationError.getErrors().add(
                    "Work Order Date must be in "
                            + DATE_FORMAT
                            + " format");
        }

        /*
         * =================================================
         * 5. WORK ORDER NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workordername",
                "Work Order Name",
                validationError);

        /*
         * =================================================
         * 6. WORK ORDER TYPE
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workordertype",
                "Work Order Type",
                validationError);

        /*
         * =================================================
         * 7. ACTIVE
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "active",
                "Active",
                validationError);

        /*
         * =================================================
         * 8. CONTRACTOR NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "contractorname",
                "Contractor Name",
                validationError);

        /*
         * =================================================
         * 9. WORK NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workname",
                "Work Name",
                validationError);

        /*
         * =================================================
         * 10. WORK CODE
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workcode",
                "Work Code",
                validationError);

        /*
         * =================================================
         * 11. TOTAL ORDER AMT
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "totalorderamt",
                "Total Order Amt",
                validationError,
                false);

        /*
         * =================================================
         * 12. ADVANCE PAYABLE
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "advancepayable",
                "Advance Payable",
                validationError,
                true);

        /*
         * =================================================
         * 13. FUND
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "fund",
                "Fund",
                validationError);

        /*
         * =================================================
         * 14. DEPARTMENT
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
         * 15. SCHEME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "scheme",
                "Scheme",
                validationError);

        /*
         * =================================================
         * 16. SANCTION DATE
         * =================================================
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
         * 17. EMD AMOUNT
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "emdamout",
                "EMD Amount",
                validationError,
                false);

        /*
         * =================================================
         * 18. BG AMOUNT
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "bgamount",
                "BG Amount",
                validationError,
                true);

        /*
         * =================================================
         * 19. APBG AMOUNT
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "apbgamount",
                "APBG Amount",
                validationError,
                true);

        return validationError;
    }

    /**
     * =====================================================
     * WORK ORDER ITEMS VALIDATION
     * =====================================================
     */
    private RowValidationError validateWorkOrderItems(
            Row row,
            int excelRowNumber,
            Map<String, Integer> headerMap) {

        RowValidationError validationError =
                new RowValidationError(excelRowNumber);

        /*
         * =================================================
         * 1. TENDER NUMBER
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "tendernumber",
                "Tender Number",
                validationError);

        /*
         * =================================================
         * 2. WORK ORDER NO.
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "workorderno",
                "Work Order No.",
                validationError);

        /*
         * =================================================
         * 3. ITEM NAME
         * =================================================
         */
        validateRequired(
                row,
                headerMap,
                "itemname",
                "Item Name",
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
         * 5. UNIT RATE
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "unitrate",
                "Unit Rate",
                validationError,
                false);

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
                false);

        /*
         * =================================================
         * 7. UNIT VALUE WITH GST
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "unitvaluewithgst",
                "Unit Value with GST",
                validationError,
                false);

        /*
         * =================================================
         * 8. QUANTITY
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "quantity",
                "Quantity",
                validationError,
                false);

        /*
         * =================================================
         * 9. AMOUNT
         * =================================================
         */
        validateNumeric(
                row,
                headerMap,
                "amount",
                "Amount",
                validationError,
                false);

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