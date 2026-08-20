package org.egov.finance.migration.modules.journalvoucher.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class AccountDetailContract {
	
	private String glcode;
	private Double debitAmount;
	private Double creditAmount;
	private FunctionContract function;
	private List<SubledgerDetailContract> subledgerDetails = new ArrayList<>();
}
