package org.egov.finance.migration.modules.bank.response;

import java.util.List;

import lombok.Data;

@Data
public class BankResponse {
   
	private Integer id;
    private String code;
    private String name;
    private String message;
    private boolean success;
    private List<?> errors;
}
