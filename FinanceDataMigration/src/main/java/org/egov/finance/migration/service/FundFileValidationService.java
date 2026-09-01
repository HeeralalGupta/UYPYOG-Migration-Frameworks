package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.FundRowValidator;
import org.springframework.stereotype.Service;

@Service
public class FundFileValidationService
        extends AbstractFileValidationService {

    public FundFileValidationService(
            FundRowValidator fundRowValidator) {

        super(fundRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.FUND;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * Your uploaded FUND.xlsx contains
         * only one sheet named "Sheet1".
         *
         * Therefore, if your application expects the first
         * sheet, use getSheetAt(0).
         */
        Sheet sheet = workbook.getSheetAt(0);

        if (sheet == null) {

            throw new IllegalArgumentException(
                    "Required Excel sheet for Fund not found.");
        }

        return sheet;
    }

    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        /*
         * Excel:
         *
         * Row 1 -> Fund Master
         * Row 2 -> Note
         * Row 3 -> Header
         * Row 4 -> First data row
         */

        for (int i = 0;
                i <= sheet.getLastRowNum();
                i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            Map<String, Integer> headers =
                    createHeaderMap(row);

            if (headers.containsKey("ulbname")
                    && headers.containsKey("fundname")
                    && headers.containsKey("natureoffund")) {

                return i;
            }
        }

        return -1;
    }

    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        /*
         * =====================================================
         * REQUIRED FUND HEADERS
         * =====================================================
         */

        String[] requiredHeaders = {
                "ulbname",
                "fundname",
                "natureoffund"
        };

        boolean valid = true;

        for (String header : requiredHeaders) {

            if (!headerMap.containsKey(header)) {

                result.getErrors().add(
                        "Required column missing: "
                                + header);

                valid = false;
            }
        }

        return valid;
    }
}