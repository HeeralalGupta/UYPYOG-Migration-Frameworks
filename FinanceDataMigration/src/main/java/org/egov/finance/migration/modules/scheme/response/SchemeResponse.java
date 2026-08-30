package org.egov.finance.migration.modules.scheme.response;

import java.util.List;

import lombok.Data;

@Data
public class SchemeResponse {
	private boolean success;
    private Long id;
    private String code;
    private String name;
    private String message;
    private List<String> errors;
}
