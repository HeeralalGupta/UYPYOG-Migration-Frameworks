package org.egov.finance.migration.modules.supplier.response;

import java.util.List;

import lombok.Data;

@Data
public class SupplierResponse {

	
	private Integer id;
    private String code;
    private String name;
    private String message;
    private boolean success;
    private List<?> errors;
}
