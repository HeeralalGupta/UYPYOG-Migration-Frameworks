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

        return checkHeaderRow(
                sheet,
                "ulbname",
                "bank",
                "branchnamelocation",
                "ifsccode",
                "branchcode",
                "address");
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