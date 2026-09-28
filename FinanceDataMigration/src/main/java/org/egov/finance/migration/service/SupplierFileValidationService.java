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
    	if (workbook == null || workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException(
                    "Supplier Excel file does not contain any sheet.");
        }
    	
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

            Sheet currentSheet = workbook.getSheetAt(i);

            if ("Supplier".equalsIgnoreCase(currentSheet.getSheetName())) {
                return currentSheet;
            }
        }

        /*
         * If only one sheet exists, use it.
         */
        if (workbook.getNumberOfSheets() == 1) {
        	Sheet singleSheet = workbook.getSheetAt(0);
        	System.out.println("Single sheet name: " + singleSheet.getSheetName());
			if ("Supplier".equalsIgnoreCase(singleSheet.getSheetName())) {
				return singleSheet;
			} else {
				throw new IllegalArgumentException(
						"Required Excel sheet for Supplier not found. " + "Expected sheet name: Supplier");
			}
           // return workbook.getSheetAt(0);
        }
            throw new IllegalArgumentException(
                    "Required Excel sheet for Supplier not found.");
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