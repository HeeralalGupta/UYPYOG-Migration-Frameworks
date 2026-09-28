package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.SchemeRowValidator;
import org.springframework.stereotype.Service;

@Service
public class SchemeFileValidationService
        extends AbstractFileValidationService {

    public SchemeFileValidationService(
            SchemeRowValidator schemeRowValidator) {

        super(schemeRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.SCHEME;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        /*
         * =====================================================
         * GET SCHEME SHEET
         * =====================================================
         *
         * The Scheme Excel contains the data in the first sheet.
         *
         * Therefore, get the first sheet.
         */
    	 if (workbook == null || workbook.getNumberOfSheets() == 0) {
             throw new IllegalArgumentException(
                     "Scheme Excel file does not contain any sheet.");
         }

         for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

             Sheet currentSheet = workbook.getSheetAt(i);

             if ("Scheme".equalsIgnoreCase(currentSheet.getSheetName())) {
                 return currentSheet;
             }
         }

         /*
          * If only one sheet exists, use it.
          */
         if (workbook.getNumberOfSheets() == 1) {
         	Sheet singleSheet = workbook.getSheetAt(0);
         	System.out.println("Single sheet name: " + singleSheet.getSheetName());
 			if ("Scheme".equalsIgnoreCase(singleSheet.getSheetName())) {
 				return singleSheet;
 			} else {
 				throw new IllegalArgumentException(
 						"Required Excel sheet for Scheme not found. " + "Expected sheet name: Scheme");
 			}
            // return workbook.getSheetAt(0);
         }
        
            throw new IllegalArgumentException(
                    "Required Excel sheet for Scheme not found.");
    }

    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        return checkHeaderRow(
                sheet,
                "ulbname",
                "schemename",
                "fund",
                "status",
                "startdate",
                "enddate");
    }

    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        /*
         * =====================================================
         * REQUIRED SCHEME HEADERS
         * =====================================================
         */

        String[] requiredHeaders = {
                "ulbname",
                "schemename",
                "fund",
                "status",
                "startdate",
                "enddate"
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