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

    	if (workbook == null || workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException(
                    "Fund Excel file does not contain any sheet.");
        }

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

            Sheet currentSheet = workbook.getSheetAt(i);

            if ("Bank Branch".equalsIgnoreCase(currentSheet.getSheetName())) {
                return currentSheet;
            }
        }

        /*
         * If only one sheet exists, use it.
         */
        if (workbook.getNumberOfSheets() == 1) {
        	Sheet singleSheet = workbook.getSheetAt(0);
        	System.out.println("Single sheet name: " + singleSheet.getSheetName());
			if ("Bank Branch".equalsIgnoreCase(singleSheet.getSheetName())) {
				return singleSheet;
			} else {
				throw new IllegalArgumentException(
						"Required Excel sheet for Bank Branch not found. " + "Expected sheet name: Bank Branch");
			}
           // return workbook.getSheetAt(0);
        }

        throw new IllegalArgumentException(
                    "Required Excel sheet for Bank Branch not found.");
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