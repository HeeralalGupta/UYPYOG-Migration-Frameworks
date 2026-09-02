package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.BankAccountRowValidator;
import org.springframework.stereotype.Service;

@Service
public class BankAccountFileValidationService
        extends AbstractFileValidationService {

    public BankAccountFileValidationService(
            BankAccountRowValidator bankAccountRowValidator) {

        super(bankAccountRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.BANK_ACCOUNT;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * =====================================================
         * GET BANK ACCOUNT SHEET
         * =====================================================
         *
         * Bank Account Excel contains the required data
         * in the first sheet.
         */

        Sheet sheet = workbook.getSheetAt(0);

        if (sheet == null) {

            throw new IllegalArgumentException(
                    "Required Excel sheet for Bank Account not found.");
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
         * Excel columns:
         *
         * Sl. No.
         * ULB Name
         * Bank Branch
         * IFSC Code
         * Account Number
         * Fund
         * Account Type
         * Description
         * Pay To
         * Usage Type
         *
         * The service searches the complete Excel sheet
         * until all required headers are found.
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
                    && headers.containsKey("bankbranch")
                    && headers.containsKey("ifsccode")
                    && headers.containsKey("accountnumber")
                    && headers.containsKey("fund")
                    && headers.containsKey("accounttype")
                    && headers.containsKey("usagetype")) {

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
         * REQUIRED BANK ACCOUNT HEADERS
         * =====================================================
         */

        String[] requiredHeaders = {

                "ulbname",

                "bankbranch",

                "ifsccode",

                "accountnumber",

                "fund",

                "accounttype",

                "usagetype"
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