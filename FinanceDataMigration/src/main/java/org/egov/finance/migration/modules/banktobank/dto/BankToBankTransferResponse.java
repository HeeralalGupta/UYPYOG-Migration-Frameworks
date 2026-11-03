package org.egov.finance.migration.modules.banktobank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankToBankTransferResponse {
	private Long voucherId;
	private String voucherNumber;
	private String message;
}
