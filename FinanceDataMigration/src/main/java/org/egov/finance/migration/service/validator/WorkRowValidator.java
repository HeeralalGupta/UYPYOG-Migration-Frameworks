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
public class WorkRowValidator implements MigrationRowValidator {

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
         * 2. NAME OF WORK
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "nameofwork",
                "Name of Work",
                validationError);

        /*
         * =====================================================
         * 3. WORK TYPE
         * =====================================================
         */

        validateRequired(
                row,
                headerMap,
                "worktype",
                "Work Type",
                validationError);

        /*
         * =====================================================
         * 4. FUND
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
         * 5. ESTIMATE VALUE
         * =====================================================
         */

        String estimateValue =
                getValue(row, headerMap, "estimatevalue");

        if (!estimateValue.isEmpty()) {

            if (!isNumeric(estimateValue)) {

                validationError.getErrors().add(
                        "Estimate Value must be numeric");

            } else {

                try {

                    double value =
                            Double.parseDouble(
                                    estimateValue.replace(",", ""));

                    if (value < 0) {

                        validationError.getErrors().add(
                                "Estimate Value cannot be negative");
                    }

                } catch (NumberFormatException e) {

                    validationError.getErrors().add(
                            "Invalid Estimate Value");
                }
            }
        }

        /*
         * =====================================================
         * 6. START DATE
         * =====================================================
         */

        String startDate =
                getValue(row, headerMap, "startdate");

        if (!startDate.isEmpty()) {

            if (!isValidDate(startDate)) {

                validationError.getErrors().add(
                        "Start Date must be in dd/MM/yyyy format");
            }
        }

        /*
         * =====================================================
         * 7. END DATE
         * =====================================================
         */

        String endDate =
                getValue(row, headerMap, "enddate");

        if (!endDate.isEmpty()) {

            if (!isValidDate(endDate)) {

                validationError.getErrors().add(
                        "End Date must be in dd/MM/yyyy format");
            }
        }

        /*
         * =====================================================
         * 8. START DATE / END DATE RELATION
         * =====================================================
         */

        if (isValidDate(startDate) && isValidDate(endDate)) {

            try {

                SimpleDateFormat sdf =
                        new SimpleDateFormat(DATE_FORMAT);

                sdf.setLenient(false);

                if (sdf.parse(endDate).before(sdf.parse(startDate))) {

                    validationError.getErrors().add(
                            "End Date cannot be before Start Date");
                }

            } catch (ParseException e) {

                // Already handled by date validation above.
            }
        }

        /*
         * =====================================================
         * RETURN
         * =====================================================
         */

        return validationError;
    }

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

        return formatter.formatCellValue(cell).trim();
    }

    private boolean isNumeric(String value) {

        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        try {

            Double.parseDouble(
                    value.replace(",", "").trim());

            return true;

        } catch (NumberFormatException e) {

            return false;
        }
    }

    private boolean isValidDate(String value) {

        if (value == null || value.trim().isEmpty()) {
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