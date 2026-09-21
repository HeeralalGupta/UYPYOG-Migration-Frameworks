package org.egov.finance.migration.modules.journalvoucher.response;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseErrorResponse {

	private ResponseInfo responseInfo;

	private String message;

	private List<String> errors;

	private Long id;

	private String billNumber;

}
