package org.egov.finance.migration.modules.bankaccount.response;

import java.util.List;

import org.egov.finance.migration.modules.bank.response.BankResponse;

import lombok.Data;

@Data
public class BankAccountResponse {
    
	private Integer id;
    private String accountNumber;
    private String ifscCode;
    private String message;
    private boolean success;
    private List<?> errors;
}
