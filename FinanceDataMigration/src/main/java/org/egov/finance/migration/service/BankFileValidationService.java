package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.BankRowValidator;
import org.springframework.stereotype.Service;

@Service
public class BankFileValidationService
        extends AbstractFileValidationService {

    public BankFileValidationService(
            BankRowValidator bankRowValidator) {

        super(bankRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.BANK;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * =====================================================
         * GET BANK SHEET
         * =====================================================
         *
         * If Bank Excel contains only one sheet,
         * use the first sheet.
         */

        Sheet sheet = workbook.getSheetAt(0);

        if (sheet == null) {

            throw new IllegalArgumentException(
                    "Required Excel sheet for Bank not found.");
        }

        return sheet;
    }

    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        /*
         * =====================================================
         * FIND HEADER ROW
         * =====================================================
         *
         * The service will search the complete Excel
         * until it finds:
         *
         * ULB Name
         * Bank Name
         * Narration
         *
         * Example:
         *
         * Row 1 -> Bank Master
         * Row 2 -> Note
         * Row 3 -> Header
         * Row 4 -> First Data
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
                    && headers.containsKey("bankname")
                    && headers.containsKey("narration")) {

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
         * REQUIRED BANK HEADERS
         * =====================================================
         */

        String[] requiredHeaders = {
                "ulbname",
                "bankname",
                "narration"
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