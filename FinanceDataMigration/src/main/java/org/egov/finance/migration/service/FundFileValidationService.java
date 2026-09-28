package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.FundRowValidator;
import org.springframework.stereotype.Service;

@Service
public class FundFileValidationService extends AbstractFileValidationService {

    public FundFileValidationService(FundRowValidator fundRowValidator) {
        super(fundRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {
        return MigrationType.FUND;
    }

    @Override
    protected Sheet getSheet(Workbook workbook) {

        if (workbook == null || workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException(
                    "Fund Excel file does not contain any sheet.");
        }

        /*
         * FUND Excel:
         *
         * If the workbook contains a sheet named "Fund",
         * use that sheet.
         *
         * If there is only one sheet, use the first sheet.
         */
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

            Sheet currentSheet = workbook.getSheetAt(i);

            if ("Fund".equalsIgnoreCase(currentSheet.getSheetName())) {
                return currentSheet;
            }
        }

        /*
         * If only one sheet exists, use it.
         */
        if (workbook.getNumberOfSheets() == 1) {
        	Sheet singleSheet = workbook.getSheetAt(0);
        	System.out.println("Single sheet name: " + singleSheet.getSheetName());
			if ("Fund".equalsIgnoreCase(singleSheet.getSheetName())) {
				return singleSheet;
			} else {
				throw new IllegalArgumentException(
						"Required Excel sheet for Fund not found. " + "Expected sheet name: Fund");
			}
           // return workbook.getSheetAt(0);
        }

        throw new IllegalArgumentException(
                "Required Excel sheet for Fund not found. "
                + "Expected sheet name: Fund");
    }

    @Override
    protected int findHeaderRow(Sheet sheet) {

        return checkHeaderRow(
                sheet,
                "ulbname",
                "fundname",
                "natureoffund");
    }

    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        /*
         * =====================================================
         * REQUIRED FUND HEADERS
         * =====================================================
         */
        String[] requiredHeaders = {
                "ulbname",
                "fundname",
                "natureoffund"
        };

        boolean valid = true;

        for (String header : requiredHeaders) {

            if (!headerMap.containsKey(header)) {

                result.getErrors().add(
                        "Required column missing: " + header);

                valid = false;
            }
        }

        return valid;
    }
}

