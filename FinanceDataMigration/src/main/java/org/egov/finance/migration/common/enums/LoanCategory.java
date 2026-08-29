package org.egov.finance.migration.common.enums;

public enum LoanCategory {

 	HOUSING("Housing Loan"),
    SELF_EMPLOYMENT("Self Employment Loan"),
    EDUCATION("Education Loan"),
    EMPLOYEE("Employee Loan"),
    INFRASTRUCTURE("Infrastructure Loan");

    private final String label;

    LoanCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}