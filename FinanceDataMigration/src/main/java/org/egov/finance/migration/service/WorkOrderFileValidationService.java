package org.egov.finance.migration.service;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.WorkOrderRowValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkOrderFileValidationService extends AbstractFileValidationService {

	private final WorkOrderRowValidator workOrderRowValidator;

	public WorkOrderFileValidationService(WorkOrderRowValidator workOrderRowValidator) {

		super(workOrderRowValidator);

		this.workOrderRowValidator = workOrderRowValidator;
	}

	/**
	 * ===================================================== MODULE CODE
	 * =====================================================
	 */
	@Override
	protected MigrationType getModuleCode() {

		return MigrationType.WORK_ORDER;
	}

	/**
	 * ===================================================== MAIN VALIDATION
	 *
	 * Work Order contains two sheets:
	 *
	 * 1. Work Order Master 2. Work Order Items
	 * =====================================================
	 */
	@Override
	public FileValidationResult validate(MultipartFile file) {

		FileValidationResult result = new FileValidationResult();

		result.setFileName(file.getOriginalFilename());

		result.setModuleCode(getModuleCode().toString());

		try (InputStream inputStream = file.getInputStream();

				Workbook workbook = WorkbookFactory.create(inputStream)) {

			/*
			 * ================================================= 1. GET WORK ORDER MASTER
			 * SHEET =================================================
			 */

			Sheet masterSheet = workbook.getSheet(ExcelConstants.WORK_ORDER_MASTER_SHEET);

			if (masterSheet == null) {

				result.setValid(false);

				result.getErrors().add("Required sheet not found: " + ExcelConstants.WORK_ORDER_MASTER_SHEET);

				return result;
			}

			/*
			 * ================================================= 2. GET WORK ORDER ITEMS
			 * SHEET =================================================
			 */

			Sheet itemsSheet = workbook.getSheet(ExcelConstants.WORK_ORDER_ITEMS_SHEET);

			if (itemsSheet == null) {

				result.setValid(false);

				result.getErrors().add("Required sheet not found: " + ExcelConstants.WORK_ORDER_ITEMS_SHEET);

				return result;
			}

			/*
			 * ================================================= 3. VALIDATE MASTER SHEET
			 * =================================================
			 */

			validateSheet(masterSheet, ExcelConstants.WORK_ORDER_MASTER_SHEET, result);

			/*
			 * ================================================= 4. VALIDATE ITEMS SHEET
			 * =================================================
			 */

			validateSheet(itemsSheet, ExcelConstants.WORK_ORDER_ITEMS_SHEET, result);

			/*
			 * ================================================= 5. FINAL RESULT
			 * =================================================
			 */

			result.setValid(result.getErrors().isEmpty() && result.getRowErrors().isEmpty());

		} catch (Exception e) {

			result.setValid(false);

			result.getErrors().add("Unable to validate file: " + e.getMessage());
		}

		return result;
	}

	/**
	 * ===================================================== VALIDATE ONE SHEET
	 * =====================================================
	 */
	private void validateSheet(

			Sheet sheet,

			String sheetName,

			FileValidationResult result) {

		/*
		 * ================================================= 1. FIND HEADER ROW
		 * =================================================
		 */

		int headerRowIndex =

				findHeaderRow(

						sheet,

						sheetName);

		if (headerRowIndex == -1) {

			result.getErrors().add(

					"Header row not found in sheet: "

							+ sheetName);

			return;
		}

		/*
		 * ================================================= 2. CREATE HEADER MAP
		 * =================================================
		 */

		Row headerRow =

				sheet.getRow(headerRowIndex);

		Map<String, Integer> headerMap =

				createHeaderMap(headerRow);

		/*
		 * ================================================= 3. VALIDATE HEADERS
		 * =================================================
		 */

		if (!validateHeaders(

				headerMap,

				sheetName,

				result)) {

			return;
		}

		/*
		 * ================================================= 4. VALIDATE DATA ROWS
		 * =================================================
		 */

		int totalRows = 0;

		/*
		 * Work Order Number must be unique only in Work Order Master sheet.
		 *
		 * Items sheet can have duplicate Work Order Numbers.
		 */
		Set<String> workOrderNumbers = new HashSet<>();

		for (int rowIndex =

				headerRowIndex + 1;

				rowIndex <= sheet.getLastRowNum();

				rowIndex++) {

			Row row =

					sheet.getRow(rowIndex);

			if (row == null

					|| isEmptyRow(row)) {

				continue;
			}

			totalRows++;

			int excelRowNumber =

					rowIndex + 1;

			/*
			 * ================================================= WORK ORDER NUMBER UNIQUE
			 * CHECK
			 *
			 * Only for Work Order Master =================================================
			 */

			if (ExcelConstants.WORK_ORDER_MASTER_SHEET.equals(sheetName)) {

				Integer workOrderNoColumn = headerMap.get("workorderno");

				if (workOrderNoColumn != null) {

					String workOrderNo =

							row.getCell(workOrderNoColumn) != null

									? row.getCell(workOrderNoColumn).toString().trim()

									: "";

					if (!workOrderNo.isEmpty()

							&& !workOrderNumbers.add(workOrderNo)) {

						RowValidationError duplicateError =

								new RowValidationError(excelRowNumber);

						duplicateError.getErrors().add(

								"[" + sheetName + "] " + "Duplicate Work Order Number: " + workOrderNo);

						result.getRowErrors().add(duplicateError);
					}
				}
			}

			/*
			 * ================================================= NORMAL ROW VALIDATION
			 * =================================================
			 */

			RowValidationError rowError =

					workOrderRowValidator.validate(

							row,

							excelRowNumber,

							headerMap);

			if (!rowError.getErrors().isEmpty()) {

				/*
				 * Add sheet name to every error.
				 */

				RowValidationError sheetRowError =

						new RowValidationError(

								excelRowNumber);

				for (String error :

				rowError.getErrors()) {

					sheetRowError.getErrors().add(

							"[" + sheetName + "] "

									+ error);
				}

				result.getRowErrors().add(

						sheetRowError);
			}
		}

		/*
		 * ================================================= 5. SET RESULT INFORMATION
		 * =================================================
		 */

		result.setHeaderRow(

				headerRowIndex + 1);

		result.setHeaderStartRow(

				headerRowIndex + 1);

		result.setHeaderEndRow(

				headerRowIndex + 1);

		result.setDataStartRow(

				headerRowIndex + 2);

		result.setColumnCount(

				headerMap.size());

		/*
		 * Add rows from both sheets.
		 */

		result.setTotalRows(

				result.getTotalRows()

						+ totalRows);
	}

	/**
	 * ===================================================== REQUIRED ABSTRACT
	 * METHOD
	 *
	 * Returns the Work Order Master sheet.
	 *
	 * This method is required because the parent class declares getSheet() as
	 * abstract. =====================================================
	 */
	@Override
	protected Sheet getSheet(Workbook workbook) {

		return workbook.getSheet(ExcelConstants.WORK_ORDER_MASTER_SHEET);
	}

	/**
	 * ===================================================== REQUIRED ABSTRACT
	 * METHOD
	 *
	 * This method is used by the parent class.
	 *
	 * For Work Order Master, find the header row using Master-specific headers.
	 * =====================================================
	 */
	@Override
	protected int findHeaderRow(Sheet sheet) {

		for (int i = 0; i <= sheet.getLastRowNum(); i++) {

			Row row = sheet.getRow(i);

			if (row == null) {
				continue;
			}

			Map<String, Integer> headers = createHeaderMap(row);

			if (headers.containsKey("ulbname") && headers.containsKey("tendernumber")
					&& headers.containsKey("workorderno") && headers.containsKey("workorderdate")) {

				return i;
			}
		}

		return -1;
	}

	/**
	 * ===================================================== FIND HEADER ROW FOR A
	 * SPECIFIC SHEET =====================================================
	 */
	private int findHeaderRow(Sheet sheet, String sheetName) {

		for (int i = 0; i <= sheet.getLastRowNum(); i++) {

			Row row = sheet.getRow(i);

			if (row == null) {
				continue;
			}

			Map<String, Integer> headers = createHeaderMap(row);

			/*
			 * --------------------------------------------- Work Order Master
			 * ---------------------------------------------
			 */
			if (ExcelConstants.WORK_ORDER_MASTER_SHEET.equals(sheetName)) {

				if (headers.containsKey("ulbname") && headers.containsKey("tendernumber")
						&& headers.containsKey("workorderno") && headers.containsKey("workorderdate")) {

					return i;
				}
			}

			/*
			 * --------------------------------------------- Work Order Items
			 * ---------------------------------------------
			 */
			if (ExcelConstants.WORK_ORDER_ITEMS_SHEET.equals(sheetName)) {

				if (headers.containsKey("tendernumber") && headers.containsKey("workorderno")
						&& headers.containsKey("itemname")) {

					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * ===================================================== REQUIRED ABSTRACT
	 * METHOD
	 *
	 * Validate Work Order Master headers.
	 *
	 * This method is required because the parent class declares validateHeaders()
	 * as abstract. =====================================================
	 */
	@Override
	protected boolean validateHeaders(Map<String, Integer> headerMap, FileValidationResult result) {

		String[] requiredHeaders = {

				"ulbname",

				"tendernumber",

				"workorderno",

				"workorderdate",

				"workordername",

				"workordertype",

				"active",

				"contractorname",

				"workname",

				"workcode",

				"fund",

				"department",

				"scheme" };

		boolean valid = true;

		for (String header : requiredHeaders) {

			if (!headerMap.containsKey(header)) {

				result.getErrors().add("Required column missing: " + header);

				valid = false;
			}
		}

		return valid;
	}

	/**
	 * ===================================================== VALIDATE HEADERS FOR
	 * SPECIFIC SHEET =====================================================
	 */
	private boolean validateHeaders(Map<String, Integer> headerMap, String sheetName, FileValidationResult result) {

		String[] requiredHeaders;

		/*
		 * --------------------------------------------- Work Order Master
		 * ---------------------------------------------
		 */
		if (ExcelConstants.WORK_ORDER_MASTER_SHEET.equals(sheetName)) {

			requiredHeaders = new String[] {

					"ulbname", "tendernumber", "workorderno", "workorderdate", "workordername", "workordertype",
					"active", "contractorname", "workname", "workcode", "fund", "department", "scheme" };

		} else {

			/*
			 * --------------------------------------------- Work Order Items
			 * ---------------------------------------------
			 */
			requiredHeaders = new String[] {

					"tendernumber", "workorderno", "itemname", "unit" };
		}

		boolean valid = true;

		for (String header : requiredHeaders) {

			if (!headerMap.containsKey(header)) {

				result.getErrors().add("Required column missing in " + sheetName + ": " + header);

				valid = false;
			}
		}

		return valid;
	}
}