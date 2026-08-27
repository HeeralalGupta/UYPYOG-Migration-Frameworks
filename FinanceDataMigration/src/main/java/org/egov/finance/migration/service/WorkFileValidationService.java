package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.WorkRowValidator;
import org.springframework.stereotype.Service;

@Service
public class WorkFileValidationService
        extends AbstractFileValidationService {

    public WorkFileValidationService(
            WorkRowValidator workRowValidator) {

        super(workRowValidator);
    }

    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.WORK;
    }

    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        return workbook.getSheetAt(0);
    }

    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        for (int i = 0;
             i <= sheet.getLastRowNum();
             i++) {

            Row row = sheet.getRow(i);

            if (row == null) {
                continue;
            }

            Map<String, Integer> headers =
                    createHeaderMap(row);

            if (headers.containsKey("ulbname")
                    && headers.containsKey("nameofwork")
                    && headers.containsKey("worktype")
                    && headers.containsKey("fund")) {

                return i;
            }
        }

        return -1;
    }

    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        String[] requiredHeaders = {
                "ulbname",
                "nameofwork",
                "worktype",
                "fund"
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