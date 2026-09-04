package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.SupplierRowValidator;
import org.springframework.stereotype.Service;

@Service
public class SupplierFileValidationService
        extends AbstractFileValidationService {

    public SupplierFileValidationService(
            SupplierRowValidator supplierRowValidator) {

        super(supplierRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.SUPPLIER;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * =====================================================
         * GET SUPPLIER SHEET
         * =====================================================
         *
         * Uploaded Supplier Excel contains one sheet:
         *
         * Sheet1
         *
         * Therefore, use the first sheet.
         */

        Sheet sheet = workbook.getSheetAt(0);

        if (sheet == null) {

            throw new IllegalArgumentException(
                    "Required Excel sheet for Supplier not found.");
        }

        return sheet;
    }

    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        return checkHeaderRow(
                sheet,
                "ulbname",
                "suppliername",
                "correspondenceaddress",
                "contactperson",
                "mobilenumber",
                "email",
                "bankname",
                "bankbranch",
                "ifsccode",
                "bankaccountnumber",
                "suppliertype",
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
         * REQUIRED SUPPLIER HEADERS
         * =====================================================
         *
         * Based on the uploaded SUPPLIER(2).xlsx:
         *
         * * indicates mandatory fields.
         */

        String[] requiredHeaders = {

                "ulbname",

                "suppliername",

                "correspondenceaddress",

                "contactperson",

                "mobilenumber",

                "email",

                "bankname",

                "bankbranch",

                "ifsccode",

                "bankaccountnumber",

                "suppliertype",

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