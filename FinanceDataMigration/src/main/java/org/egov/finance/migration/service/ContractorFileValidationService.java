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

        /*
         * =====================================================
         * FIND CONTRACTOR HEADER ROW
         * =====================================================
         *
         * Uploaded Excel:
         *
         * Row 1 -> Contractor Details
         * Row 2 -> Note
         * Row 3 -> Header
         * Row 4 -> First Data
         *
         * We search dynamically instead of hard-coding
         * row number.
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
             * Check important Contractor headers.
             */
            if (headers.containsKey("ulbname")
                    && headers.containsKey("contractorname")
                    && headers.containsKey("correspondenceaddress")
                    && headers.containsKey("contactperson")
                    && headers.containsKey("mobilenumber")
                    && headers.containsKey("email")
                    && headers.containsKey("bankname")
                    && headers.containsKey("bankbranch")
                    && headers.containsKey("ifsccode")
                    && headers.containsKey("bankaccountnumber")
                    && headers.containsKey("contractortype")
                    && headers.containsKey("source")
                    && headers.containsKey("status")
                    && headers.containsKey("pannumber")) {

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