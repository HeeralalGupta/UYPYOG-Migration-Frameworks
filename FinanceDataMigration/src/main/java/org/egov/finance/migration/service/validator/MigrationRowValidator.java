package org.egov.finance.migration.service.validator;

import org.apache.poi.ss.usermodel.Row;
import org.egov.finance.migration.common.dto.RowValidationError;

import java.util.Map;

public interface MigrationRowValidator {

	RowValidationError validate(Row row, int excelRowNumber, Map<String, Integer> headerMap);
}