package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.ContractorRowValidator;
import org.springframework.stereotype.Service;

@Service
public class ContractorFileValidationService
        extends AbstractFileValidationService {

    public ContractorFileValidationService(
            ContractorRowValidator contractorRowValidator) {

        super(contractorRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.CONTRACTOR;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * =====================================================
         * GET CONTRACTOR SHEET
         * =====================================================
         *
         * Uploaded CONTRACTOR Excel contains one sheet:
         *
         * Sheet1
         *
         * Therefore, get the first sheet.
         */

        Sheet sheet = workbook.getSheetAt(0);

        if (sheet == null) {

            throw new IllegalArgumentException(
                    "Required Excel sheet for Contractor not found.");
        }

        return sheet;
    }

    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        return checkHeaderRow(
                sheet,
                "ulbname",
                "contractorname",
                "correspondenceaddress",
                "contactperson",
                "mobilenumber",
                "email",
                "bankname",
                "bankbranch",
                "ifsccode",
                "bankaccountnumber",
                "contractortype",
                "source",
                "status",
                "pannumber");
    }

    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        /*
         * =====================================================
         * REQUIRED CONTRACTOR HEADERS
         * =====================================================
         *
         * These columns are marked with * in the
         * uploaded Contractor Excel.
         */

        String[] requiredHeaders = {

                "ulbname",

                "contractorname",

                "correspondenceaddress",

                "contactperson",

                "mobilenumber",

                "email",

                "bankname",

                "bankbranch",

                "ifsccode",

                "bankaccountnumber",

                "contractortype",

                "source",

                "status",

                "pannumber"
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