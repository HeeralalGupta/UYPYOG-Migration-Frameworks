package org.egov.finance.migration.modules.deductionpayment.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DeductionPaymentResponse {

	private boolean success;
	private String message;
	private String paymentVoucherNumber;
	private Long paymentVoucherId;
	private List<String> errors = new ArrayList<>();

	public static DeductionPaymentResponse success(String message, String voucherNumber, Long voucherId) {

		DeductionPaymentResponse response = new DeductionPaymentResponse();

		response.setSuccess(true);
		response.setMessage(message);
		response.setPaymentVoucherNumber(voucherNumber);
		response.setPaymentVoucherId(voucherId);

		return response;
	}

	public static DeductionPaymentResponse failure(String message, List<String> errors) {

		DeductionPaymentResponse response = new DeductionPaymentResponse();

		response.setSuccess(false);
		response.setMessage(message);

		if (errors != null) {
			response.setErrors(errors);
		}

		return response;
	}
}
