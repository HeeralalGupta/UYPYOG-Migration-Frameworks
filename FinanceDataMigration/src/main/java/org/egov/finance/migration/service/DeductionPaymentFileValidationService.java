package org.egov.finance.migration.service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.DeductionPaymentRowValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class DeductionPaymentFileValidationService extends AbstractFileValidationService {

	/*
	 * Excel Row 3 = Java index 2. Excel Row 4 = first data row.
	 */
	private static final int HEADER_ROW_INDEX = 2;

	private final DeductionPaymentRowValidator rowValidator;

	public DeductionPaymentFileValidationService(DeductionPaymentRowValidator rowValidator) {

		super(rowValidator);
		this.rowValidator = rowValidator;
	}

	@Override
	public FileValidationResult validate(MultipartFile file) {

		FileValidationResult result = new FileValidationResult();

		if (file == null || file.isEmpty()) {
			result.setValid(false);
			result.getErrors().add("Excel file is required and cannot be empty.");
			return result;
		}

		result.setFileName(file.getOriginalFilename());
		result.setModuleCode(getModuleCode().toString());

		try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {

			FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

			Sheet sheet = getSheet(workbook);

			if (sheet == null) {
				result.setValid(false);
				result.getErrors().add("Deduction Payment sheet not found. " + "Please use the correct Excel template.");
				return result;
			}

			int headerRowIndex = findHeaderRow(sheet);

			if (headerRowIndex == -1) {
				result.setValid(false);
				result.getErrors().add("Required headers were not found in Excel row " + (HEADER_ROW_INDEX + 1)
						+ ". Please upload the correct Deduction Payment template.");
				return result;
			}

			result.setHeaderRow(headerRowIndex + 1);
			result.setHeaderStartRow(headerRowIndex + 1);
			result.setHeaderEndRow(headerRowIndex + 1);

			Row headerRow = sheet.getRow(headerRowIndex);

			Map<String, Integer> headerMap = createHeaderMap(headerRow);

			result.setColumnCount(headerMap.size());

			if (!validateHeaders(headerMap, result)) {
				result.setValid(false);
				return result;
			}

			result.setDataStartRow(headerRowIndex + 2);

			int totalRows = 0;

			for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

				Row row = sheet.getRow(rowIndex);

				if (row == null || isEmptyRow(row)) {
					continue;
				}

				totalRows++;

				int excelRowNumber = rowIndex + 1;
				evaluateFormulaCells(row, formulaEvaluator);
				RowValidationError rowError = rowValidator.validate(row, excelRowNumber, headerMap);

				if (rowError != null && !rowError.getErrors().isEmpty()) {
					result.getRowErrors().add(rowError);
				}
			}

			result.setTotalRows(totalRows);

			if (totalRows == 0) {
				result.setValid(false);
				result.getErrors().add("At least one deduction payment data row is required.");
				return result;
			}

			result.setValid(result.getErrors().isEmpty() && result.getRowErrors().isEmpty());

		} catch (Exception e) {

			result.setValid(false);
			result.getErrors().add("Unable to validate Deduction Payment Excel file: " + getRootCauseMessage(e));
		}

		return result;
	}

	@Override
	protected MigrationType getModuleCode() {
		return MigrationType.DEDUCTION_PAYMENT;
	}

	@Override
	protected Sheet getSheet(Workbook workbook) {

		if (workbook.getNumberOfSheets() == 0) {
			throw new IllegalArgumentException("Excel workbook does not contain any sheets.");
		}

		Sheet sheet = workbook.getSheet("Deduction Payment");

		if (sheet == null) {
			// Allow a single-sheet workbook even if its sheet name differs.
			if (workbook.getNumberOfSheets() == 1) {
				return workbook.getSheetAt(0);
			}
		}

		return sheet;
	}

	@Override
	protected int findHeaderRow(Sheet sheet) {

		if (sheet.getLastRowNum() < HEADER_ROW_INDEX) {
			return -1;
		}

		Row headerRow = sheet.getRow(HEADER_ROW_INDEX);

		if (headerRow == null) {
			return -1;
		}

		Map<String, Integer> headerMap = createHeaderMap(headerRow);

		return hasAllRequiredHeaders(headerMap) ? HEADER_ROW_INDEX : -1;
	}

	private boolean hasAllRequiredHeaders(Map<String, Integer> headerMap) {

		String[] requiredHeaders = { "sn", "ulbname", "voucherdate", "vouchernumber",
				"vouchertype", "recoveryname", "fund", "deductionamount", "paymentdate", "bank", "branch",
				"bankaccountnumber", "modeofpayment", "department", "function" };

		for (String header : requiredHeaders) {
			if (!headerMap.containsKey(header)) {
				return false;
			}
		}

		return true;
	}

	@Override
	protected boolean validateHeaders(Map<String, Integer> headerMap, FileValidationResult result) {

		boolean valid = true;

		valid &= validateHeader(headerMap, "sn", "SL No.", result);
		valid &= validateHeader(headerMap, "ulbname", "ULB Name", result);
		valid &= validateHeader(headerMap, "voucherdate", "Voucher Date", result);
		valid &= validateHeader(headerMap, "vouchernumber", "Voucher Number", result);
		valid &= validateHeader(headerMap, "vouchertype", "Voucher Type", result);
		valid &= validateHeader(headerMap, "recoveryname", "Recovery Name", result);
		valid &= validateHeader(headerMap, "fund", "Fund", result);
		valid &= validateHeader(headerMap, "deductionamount", "Deduction Amount", result);
		valid &= validateHeader(headerMap, "paymentdate", "Payment Date", result);
		valid &= validateHeader(headerMap, "bank", "Bank", result);
		valid &= validateHeader(headerMap, "branch", "Branch", result);
		valid &= validateHeader(headerMap, "bankaccountnumber", "Bank Account Number", result);
		valid &= validateHeader(headerMap, "modeofpayment", "Mode Of Payment", result);
		valid &= validateHeader(headerMap, "department", "Department", result);
		valid &= validateHeader(headerMap, "function", "Function", result);

		return valid;
	}

//    @Override
//    protected Map<String, Integer> createHeaderMap(Row headerRow) {
//
//        Map<String, Integer> headerMap = new HashMap<>();
//
//        if (headerRow == null) {
//            return headerMap;
//        }
//
//        /*
//         * Fixed column positions from the Deduction Payment template.
//         * These positions avoid issues caused by duplicate or renamed headers.
//         */
//        Map<Integer, String> columnKeys = new HashMap<>();
//
//        columnKeys.put(0, "sn");
//        columnKeys.put(1, "ulbname");
//        columnKeys.put(2, "referencevoucherdate");
//        columnKeys.put(3, "referencevouchernumber");
//        columnKeys.put(4, "referencevouchertype");
//        columnKeys.put(5, "recovery");
//        columnKeys.put(6, "fund");
//        columnKeys.put(7, "deductionamount");
//        columnKeys.put(8, "paymentdate");
//        columnKeys.put(9, "bank");
//        columnKeys.put(10, "branch");
//        columnKeys.put(11, "bankaccountnumber");
//        columnKeys.put(12, "modeofpayment");
//        columnKeys.put(13, "department");
//        columnKeys.put(14, "function");
//
//        for (Map.Entry<Integer, String> entry : columnKeys.entrySet()) {
//
//            int columnIndex = entry.getKey();
//            String expectedKey = entry.getValue();
//
//            Cell cell = headerRow.getCell(columnIndex);
//
//            if (cell == null) {
//                continue;
//            }
//
//            String actualHeader = normalize(cell.toString());
//
//            if (!actualHeader.isEmpty()) {
//                headerMap.put(actualHeader, columnIndex);
//            }
//        }
//
//        return headerMap;
//    }

	@Override
	protected Map<String, Integer> createHeaderMap(Row headerRow) {

		Map<String, Integer> headerMap = new HashMap<>();

		if (headerRow == null) {
			return headerMap;
		}

		String[] expectedHeaders = { "slno", "ulbname", "voucherdate", "vouchernumber",
				"vouchertype", "recoveryname", "fund", "deductionamount", "paymentdate", "bank", "branch",
				"bankaccountnumber", "modeofpayment", "department", "function" };

		String[] headerKeys = { "sn", "ulbname", "voucherdate", "vouchernumber",
				"vouchertype", "recoveryname", "fund", "deductionamount", "paymentdate", "bank", "branch",
				"bankaccountnumber", "modeofpayment", "department", "function" };

		for (int i = 0; i < expectedHeaders.length; i++) {

			Cell cell = headerRow.getCell(i);

			if (cell == null) {
				continue;
			}

			String actualHeader = normalize(cell.toString());

			if (expectedHeaders[i].equals(actualHeader)) {
				headerMap.put(headerKeys[i], i);
			}
		}

		return headerMap;
	}

	private boolean validateHeader(Map<String, Integer> headerMap, String key, String displayName,
			FileValidationResult result) {

		if (!headerMap.containsKey(key)) {
			result.getErrors().add("Missing or incorrect column: " + displayName);
			return false;
		}

		return true;
	}

	private String normalize(String value) {

		if (value == null) {
			return "";
		}

		return value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	protected boolean isEmptyRow(Row row) {

		for (int columnIndex = 0; columnIndex < 15; columnIndex++) {
			Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

			if (cell != null && !cell.toString().trim().isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private void evaluateFormulaCells(Row row, FormulaEvaluator formulaEvaluator) {

		if (row == null) {
			return;
		}

		for (Cell cell : row) {
			if (cell != null && cell.getCellType() == CellType.FORMULA) {
				formulaEvaluator.evaluateFormulaCell(cell);
			}
		}
	}

	private String getRootCauseMessage(Throwable throwable) {

		Throwable rootCause = throwable;

		while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
			rootCause = rootCause.getCause();
		}

		return rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName();
	}
}