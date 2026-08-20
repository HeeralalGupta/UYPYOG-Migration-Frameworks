package org.egov.finance.migration.common.dto;

import java.util.ArrayList;
import java.util.List;

public class FileValidationResult {

	private boolean valid;

	private String moduleCode;

	private String fileName;

	private int totalRows;
	private int headerRow;
	private int columnCount;
	private int headerStartRow;
	private int headerEndRow;
	private int dataStartRow;

	private List<String> errors = new ArrayList<>();

	private List<String> warnings = new ArrayList<>();

	private List<RowValidationError> rowErrors = new ArrayList<RowValidationError>();

	public FileValidationResult() {
	}

	public FileValidationResult(boolean valid, String moduleCode) {
		this.valid = valid;
		this.moduleCode = moduleCode;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getModuleCode() {
		return moduleCode;
	}

	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getTotalRows() {
		return totalRows;
	}

	public void setTotalRows(int totalRows) {
		this.totalRows = totalRows;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	public int getHeaderRow() {
		return headerRow;
	}

	public void setHeaderRow(int headerRow) {
		this.headerRow = headerRow;
	}

	public int getColumnCount() {
		return columnCount;
	}

	public void setColumnCount(int columnCount) {
		this.columnCount = columnCount;
	}

	public int getHeaderStartRow() {
		return headerStartRow;
	}

	public void setHeaderStartRow(int headerStartRow) {
		this.headerStartRow = headerStartRow;
	}

	public int getHeaderEndRow() {
		return headerEndRow;
	}

	public void setHeaderEndRow(int headerEndRow) {
		this.headerEndRow = headerEndRow;
	}

	public int getDataStartRow() {
		return dataStartRow;
	}

	public void setDataStartRow(int dataStartRow) {
		this.dataStartRow = dataStartRow;
	}
	
	public List<RowValidationError> getRowErrors() {
	    return rowErrors;
	}

	public void setRowErrors(
	        List<RowValidationError> rowErrors) {

	    this.rowErrors = rowErrors;
	}

}