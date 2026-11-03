
package org.egov.finance.migration.service;

import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.BankToBankTransferRowValidator;
import org.springframework.stereotype.Service;

@Service
public class BankToBankTransferFileValidationService extends AbstractFileValidationService {

	public BankToBankTransferFileValidationService(BankToBankTransferRowValidator bankToBankTransferRowValidator) {

		super(bankToBankTransferRowValidator);
	}

	@Override
	protected MigrationType getModuleCode() {

		return MigrationType.BANK_TO_BANK_TRANSFER;
	}

	@Override
	protected Sheet getSheet(Workbook workbook) {

		Sheet sheet = null;

		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {

			if (ExcelConstants.BANK_TO_BANK_TRANSFER_SHEET.equalsIgnoreCase(workbook.getSheetName(i))) {

				sheet = workbook.getSheetAt(i);
				break;
			}
		}

		if (sheet == null) {

			throw new IllegalArgumentException(
					"Required Excel sheet '" + ExcelConstants.BANK_TO_BANK_TRANSFER_SHEET + "' not found.");
		}

		return sheet;
	}

	@Override
	protected int findHeaderRow(Sheet sheet) {

		for (int i = 0; i <= sheet.getLastRowNum(); i++) {

			Row row = sheet.getRow(i);

			if (row == null) {
				continue;
			}

			Map<String, Integer> headers = createHeaderMap(row);

			if (headers.containsKey("ulbname") && headers.containsKey("voucherdate") && headers.containsKey("fromfund")
					&& headers.containsKey("fromdepartment") && headers.containsKey("function")
					&& headers.containsKey("frombank") && headers.containsKey("fromaccountno")
					&& headers.containsKey("tofund") && headers.containsKey("tobank")
					&& headers.containsKey("toaccountno")) {

				return i;
			}
		}

		return -1;
	}

	@Override
	protected boolean validateHeaders(Map<String, Integer> headerMap, FileValidationResult result) {

		String[] requiredHeaders = {

				"ulbname", "voucherdate", "fromfund", "fromdepartment", "function", "frombank", "fromaccountno",
				"tofund", "tobank", "toaccountno", "modeoftransaction", "referencenumber", "referencedate",
				"amountrs" };

		boolean valid = true;

		for (String header : requiredHeaders) {

			if (!headerMap.containsKey(header)) {

				result.getErrors().add("Required column missing: " + header);

				valid = false;
			}
		}

		return valid;
	}
}
