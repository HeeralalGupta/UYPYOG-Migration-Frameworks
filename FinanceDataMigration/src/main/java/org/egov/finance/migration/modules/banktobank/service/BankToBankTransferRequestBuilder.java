
package org.egov.finance.migration.modules.banktobank.service;

import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferRecord;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferRequest;
import org.springframework.stereotype.Service;

@Service
public class BankToBankTransferRequestBuilder {

	/**
	 * Build BankToBankTransferRequest from one BankToBankTransferRecord.
	 */
	public BankToBankTransferRequest build(BankToBankTransferRecord record) {

		BankToBankTransferRequest request = new BankToBankTransferRequest();

		/*
		 * Transfer information
		 */

		request.setVoucherDate(formatDate(record.getVoucherDate()));

		/*
		 * Transfer From
		 */

		request.setFromFund(record.getFromFund());

		request.setFromSchemeName(record.getFromSubScheme());

		request.setFromDepartmentName(record.getFromDepartment());

		request.setFunctionName(record.getFunction());

		request.setFromBankName(record.getFromBank());

		request.setFromBankAccountNumber(record.getFromAccountNumber());

		/*
		 * Transfer To
		 */

		request.setToFund(record.getToFund());

		request.setToDepartmentName(record.getToDepartment());

		request.setToBankName(record.getToBank());

		request.setToBankAccountNumber(record.getToAccountNumber());

		/*
		 * Inter-fund transfer details
		 */

		request.setFromInterFundCode(record.getSourceInterFundCode());

		request.setToInterFundCode(record.getDestinationInterFundCode());

		/*
		 * Transaction details
		 */

		request.setModeOfCollection(record.getModeOfTransaction());

		request.setReferenceNumber(record.getReferenceNumber());

		request.setReferenceDate(formatDate(record.getReferenceDate()));

		request.setAmount(record.getAmount());

		request.setDescription(record.getNarration());

		return request;
	}

	/**
	 * Convert Date to dd/MM/yyyy format.
	 */
	private String formatDate(java.util.Date date) {

		if (date == null) {
			return null;
		}

		return new java.text.SimpleDateFormat("dd/MM/yyyy").format(date);
	}
}
