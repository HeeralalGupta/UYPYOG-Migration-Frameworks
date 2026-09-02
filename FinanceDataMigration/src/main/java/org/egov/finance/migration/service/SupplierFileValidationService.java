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

        /*
         * =====================================================
         * FIND SUPPLIER HEADER ROW
         * =====================================================
         *
         * Uploaded Excel structure:
         *
         * Row 1 -> Supplier Details
         * Row 2 -> Note
         * Row 3 -> Header
         * Row 4 -> First data row
         *
         * We search dynamically instead of hard-coding
         * row number 3.
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
             * Required columns from Supplier Excel.
             *
             * ULB Name
             * Supplier Name
             * Correspondence Address
             * Contact Person
             * Mobile Number
             * Email
             * Bank Name
             * Bank Branch
             * IFSC Code
             * Bank Account Number
             * Supplier Type
             * Source
             * Status
             * PAN Number
             */

            if (headers.containsKey("ulbname")
                    && headers.containsKey("suppliername")
                    && headers.containsKey("correspondenceaddress")
                    && headers.containsKey("contactperson")
                    && headers.containsKey("mobilenumber")
                    && headers.containsKey("email")
                    && headers.containsKey("bankname")
                    && headers.containsKey("bankbranch")
                    && headers.containsKey("ifsccode")
                    && headers.containsKey("bankaccountnumber")
                    && headers.containsKey("suppliertype")
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