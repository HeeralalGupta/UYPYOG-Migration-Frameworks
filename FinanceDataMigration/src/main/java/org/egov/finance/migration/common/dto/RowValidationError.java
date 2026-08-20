package org.egov.finance.migration.common.dto;

import java.util.ArrayList;
import java.util.List;

public class RowValidationError {

	private int rowNumber;

	private List<String> errors = new ArrayList<String>();

	public RowValidationError() {
	}

	public RowValidationError(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public int getRowNumber() {
		return rowNumber;
	}

	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}

	public List<String> getErrors() {
		return errors;
	}

	public void setErrors(List<String> errors) {
		this.errors = errors;
	}
}