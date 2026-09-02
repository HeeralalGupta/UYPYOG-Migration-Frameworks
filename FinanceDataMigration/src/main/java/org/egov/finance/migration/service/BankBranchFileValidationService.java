package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.BankBranchRowValidator;
import org.springframework.stereotype.Service;

@Service
public class BankBranchFileValidationService
        extends AbstractFileValidationService {

    public BankBranchFileValidationService(
            BankBranchRowValidator bankBranchRowValidator) {

        super(bankBranchRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.BANK_BRANCH;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * =====================================================
         * GET BANK BRANCH SHEET
         * =====================================================
         *
         * Bank Branch Excel contains the required data
         * in the first sheet.
         */

        Sheet sheet = workbook.getSheetAt(0);

        if (sheet == null) {

            throw new IllegalArgumentException(
                    "Required Excel sheet for Bank Branch not found.");
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
         * Actual Bank Branch Excel headers:
         *
         * Sl. No.
         * ULB Name *
         * Bank *
         * Branch Name/Location *
         * IFSC Code *
         * Branch Code *
         * MICR
         * Address *
         * Contact Person
         * Phone Number
         * Narration
         *
         * createHeaderMap() should normalize them to:
         *
         * ulbname
         * bank
         * branchnamelocation
         * ifsccode
         * branchcode
         * micr
         * address
         * contactperson
         * phonenumber
         * narration
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

            /*
             * Check mandatory Bank Branch columns.
             */

            if (headers.containsKey("ulbname")
                    && headers.containsKey("bank")
                    && headers.containsKey("branchnamelocation")
                    && headers.containsKey("ifsccode")
                    && headers.containsKey("branchcode")
                    && headers.containsKey("address")) {

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
         * REQUIRED BANK BRANCH HEADERS
         * =====================================================
         *
         * Based on the uploaded Excel:
         *
         * ULB Name *
         * Bank *
         * Branch Name/Location *
         * IFSC Code *
         * Branch Code *
         * Address *
         */

        String[] requiredHeaders = {

                "ulbname",

                "bank",

                "branchnamelocation",

                "ifsccode",

                "branchcode",

                "address"
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