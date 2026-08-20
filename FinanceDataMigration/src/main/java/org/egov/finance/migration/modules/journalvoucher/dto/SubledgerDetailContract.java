package org.egov.finance.migration.modules.journalvoucher.dto;

import lombok.Data;

@Data
public class SubledgerDetailContract {
	
	private AccountDetailTypeContract accountDetailType;
	private AccountDetailKeyContract accountDetailKey;

	private Double amount;
}
