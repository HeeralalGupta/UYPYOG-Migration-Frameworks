package org.egov.finance.migration.modules.bankbranch.response;

import java.util.List;

import lombok.Data;
@Data
public class BankBranchResponse {

	private Integer id;
    private String branchCode;
    private String branchName;
    private String message;
    private boolean success;
    private List<?> errors;
}
