package org.egov.finance.migration.modules.billpayment.service;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.billpayment.dto.BillPaymentVoucherRecord;
import org.egov.finance.migration.modules.billpayment.dto.PaymentBillRecord;
import org.egov.finance.migration.modules.billpayment.dto.PaymentCreateRequest;
import org.springframework.stereotype.Service;

@Service
public class BillPaymentVoucherRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;

	public BillPaymentVoucherRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
		this.requestInfoBuilder = requestInfoBuilder;
	}

	public PaymentCreateRequest build(BillPaymentVoucherRecord record, MigrationRequest migrationRequest) {

		if (record == null || record.getBills() == null || record.getBills().isEmpty()) {
			throw new IllegalArgumentException("Payment voucher must contain at least one bill.");
		}

		PaymentCreateRequest request = PaymentCreateRequest.builder().paymentVoucherDate(record.getPaymentVoucherDate())
				.fund(record.getFund()).function(record.getFunction()).department(record.getDepartment())
				.paymentMode(record.getPaymentMode()).bankName(record.getBankName())
				.bankBranchName(record.getBankBranchName()).ifscCode(record.getIfscCode())
				.bankAccountNumber(record.getBankAccountNumber()).description(record.getDescription()).build();

		for (PaymentBillRecord bill : record.getBills()) {
			request.getBills().add(PaymentCreateRequest.PaymentBillRequest.builder().billNumber(bill.getBillNumber())
					.paymentAmount(bill.getPaymentAmount()).build());
		}

		return request;
	}

	public ApiRequest<PaymentCreateRequest> buildApiRequest(PaymentCreateRequest paymentRequest,
			MigrationRequest migrationRequest) {

		String tenantId = migrationRequest.getTenantId();

		ApiRequest<PaymentCreateRequest> apiRequest = new ApiRequest<>();

		apiRequest.setRequestInfo(requestInfoBuilder.build(tenantId));
		apiRequest.setTenantId(tenantId);
		apiRequest.setRequest(paymentRequest);

		return apiRequest;
	}
}
