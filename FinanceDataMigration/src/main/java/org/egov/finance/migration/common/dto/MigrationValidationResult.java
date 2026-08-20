package org.egov.finance.migration.common.dto;

import java.util.ArrayList;
import java.util.List;

public class MigrationValidationResult {

    private boolean valid;

    private String moduleCode;

    private String fileName;

    private int headerRow;

    private int dataRows;

    private int columnCount;

    private List<String> errors = new ArrayList<String>();

    private List<String> warnings = new ArrayList<String>();

    private List<String> foundColumns = new ArrayList<String>();

    private List<String> missingColumns = new ArrayList<String>();

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

    public int getHeaderRow() {
        return headerRow;
    }

    public void setHeaderRow(int headerRow) {
        this.headerRow = headerRow;
    }

    public int getDataRows() {
        return dataRows;
    }

    public void setDataRows(int dataRows) {
        this.dataRows = dataRows;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(int columnCount) {
        this.columnCount = columnCount;
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

    public List<String> getFoundColumns() {
        return foundColumns;
    }

    public void setFoundColumns(List<String> foundColumns) {
        this.foundColumns = foundColumns;
    }

    public List<String> getMissingColumns() {
        return missingColumns;
    }

    public void setMissingColumns(List<String> missingColumns) {
        this.missingColumns = missingColumns;
    }
}