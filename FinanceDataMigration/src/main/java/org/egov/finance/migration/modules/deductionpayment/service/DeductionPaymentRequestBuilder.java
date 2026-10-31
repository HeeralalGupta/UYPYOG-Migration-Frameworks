package org.egov.finance.migration.modules.deductionpayment.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.deductionpayment.dto.CreateDeductionPaymentRequest;
import org.egov.finance.migration.modules.deductionpayment.dto.DeductionPaymentRecord;
import org.egov.finance.migration.modules.deductionpayment.dto.DeductionRemittanceRequest;
import org.springframework.stereotype.Service;

@Service
public class DeductionPaymentRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;

	public DeductionPaymentRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
		this.requestInfoBuilder = requestInfoBuilder;
	}

	/**
	 * One DeductionPaymentRecord = One Deduction Remittance API Request.
	 */
	public CreateDeductionPaymentRequest build(DeductionPaymentRecord record, MigrationRequest migrationRequest) {

		if (record == null) {
			throw new IllegalArgumentException("DeductionPaymentRecord is null.");
		}

		if (record.getSerialNumber() == null) {
			throw new IllegalArgumentException("Serial Number (SN) is missing.");
		}

		if (migrationRequest == null) {
			throw new IllegalArgumentException("MigrationRequest is null for SN: " + record.getSerialNumber());
		}

		String tenantId = migrationRequest.getTenantId();

		if (!hasValue(tenantId)) {
			throw new IllegalArgumentException("Tenant ID is missing for SN: " + record.getSerialNumber());
		}

		try {

			/*
			 * ===================== 1. BUILD MAIN REQUEST	================================
			 */

			CreateDeductionPaymentRequest request = new CreateDeductionPaymentRequest();
			request.setTenantId(tenantId);
			RequestInfo requestInfo = requestInfoBuilder.build(tenantId);

			if (requestInfo == null) {
				throw new IllegalArgumentException("RequestInfo could not be created for tenant: " + tenantId);
			}

			requestInfo.setAction("_create");
			request.setRequestInfo(requestInfo);

			/*
			 * ===================================================== 2. BUILD DEDUCTION
			 * REMITTANCE REQUEST =====================================================
			 */

			DeductionRemittanceRequest remittanceRequest = buildDeductionRemittanceRequest(record);

			if (remittanceRequest == null) {
				throw new IllegalArgumentException("DeductionRemittanceRequest could not be created.");
			}

			/*
			 * ===================================================== 3. VALIDATE REQUEST
			 * =====================================================
			 */

			validateDeductionRemittanceRequest(remittanceRequest, record);

			request.setDeductionRemittanceRequest(remittanceRequest);
			return request;

		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("SN: " + record.getSerialNumber() + ": " + e.getMessage(), e);
		} catch (Exception e) {
			throw new IllegalArgumentException("SN: " + record.getSerialNumber()+ ": Unable to build Deduction Payment API request. " + e.getMessage(), e);
		}
	}

	/**
	 * Build deductionRemittanceRequest.
	 *
	 * JSON:
	 *
	 * "deductionRemittanceRequest": { "referenceVoucherDate": "2023-09-26",
	 * "referenceVoucherNumber": "1/PJ/00000002/09/2023-24", "referenceVoucherType":
	 * "Supplier Journal", "recovery": "SGST", "fund": "Municipal Fund",
	 * "deductionAmount": 1003.80, "paymentDate": "2026-10-29", "bank": "State Bank
	 * of India", "branch": "Ranchi", "bankAccountNumber": "1234567890",
	 * "modeOfPayment": "CHEQUE", "department": "Account Branch", "function":
	 * "General Administration" }
	 */
	private DeductionRemittanceRequest buildDeductionRemittanceRequest(DeductionPaymentRecord record) {

		if (record == null) {
			throw new IllegalArgumentException("DeductionPaymentRecord is null.");
		}

		DeductionRemittanceRequest request = new DeductionRemittanceRequest();

		/*
		 * Reference Voucher Details
		 */
		request.setReferenceVoucherDate(convertToApiDate(record.getVoucherDate(), "Reference Voucher Date"));
		request.setReferenceVoucherNumber(record.getVoucherNumber());
		request.setReferenceVoucherType(record.getVoucherType());

		/*
		 * Recovery and Fund
		 */
		request.setRecovery(record.getRecovery());
		request.setFund(record.getFund());

		/*
		 * Deduction Amount
		 */
		request.setDeductionAmount(record.getDeductionAmount());

		/*
		 * Payment Details
		 */
		request.setPaymentDate(convertToApiDate(record.getPaymentDate(), "Payment Date"));
		request.setBank(record.getBank());
		request.setBranch(record.getBranch());
		request.setBankAccountNumber(record.getBankAccountNumber());
		request.setModeOfPayment(normalizeModeOfPayment(record.getModeOfPayment()));

		/*
		 * MIS Details
		 */
		request.setDepartment(record.getDepartment());
		request.setFunction(record.getFunction());

		return request;
	}

	/**
	 * Validate mandatory deduction payment fields.
	 */
	private void validateDeductionRemittanceRequest(DeductionRemittanceRequest request, DeductionPaymentRecord record) {

		if (request == null) {
			throw new IllegalArgumentException("Deduction Remittance Request is null.");
		}

		/*
		 * Reference Voucher
		 */
		if (request.getReferenceVoucherDate() == null) {
			throw new IllegalArgumentException(" Voucher Date is required.");
		}

		if (!hasValue(request.getReferenceVoucherNumber())) {
			throw new IllegalArgumentException("Voucher Number is required.");
		}

		if (!hasValue(request.getReferenceVoucherType())) {
			throw new IllegalArgumentException("Voucher Type is required.");
		}

		/*
		 * Recovery and Fund
		 */
		if (!hasValue(request.getRecovery())) {
			throw new IllegalArgumentException("Recovery is required.");
		}

		if (!hasValue(request.getFund())) {
			throw new IllegalArgumentException("Fund is required.");
		}

		/*
		 * Deduction Amount
		 */
		BigDecimal amount = request.getDeductionAmount();

		if (amount == null) {
			throw new IllegalArgumentException("Deduction Amount is required.");
		}

		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Deduction Amount must be greater than zero. Value: " + amount);
		}

		/*
		 * Payment Details
		 */
		if (request.getPaymentDate() == null) {
			throw new IllegalArgumentException("Payment Date is required.");
		}

		if (!hasValue(request.getBank())) {
			throw new IllegalArgumentException("Bank is required.");
		}

		if (!hasValue(request.getBranch())) {
			throw new IllegalArgumentException("Branch is required.");
		}

		if (!hasValue(request.getBankAccountNumber())) {
			throw new IllegalArgumentException("Bank Account Number is required.");
		}

		if (!hasValue(request.getModeOfPayment())) {
			throw new IllegalArgumentException("Mode Of Payment is required.");
		}

		/*
		 * MIS Details
		 */
//		if (!hasValue(request.getDepartment())) {
//			throw new IllegalArgumentException("Department is required.");
//		}
//
//		if (!hasValue(request.getFunction())) {
//			throw new IllegalArgumentException("Function is required.");
//		}

		/*
		 * Validate supported payment modes.
		 */
		validateModeOfPayment(request.getModeOfPayment());
	}

	/**
	 * Convert Excel Date into API LocalDate.
	 *
	 * API format: yyyy-MM-dd
	 */
	private LocalDate convertToApiDate(Date date, String fieldName) {

		if (date == null) {
			throw new IllegalArgumentException(fieldName + " is missing.");
		}
		try {
			return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid " + fieldName + ": " + date, e);
		}
	}

	/**
	 * Normalize payment mode.
	 */
	private String normalizeModeOfPayment(String modeOfPayment) {

		if (!hasValue(modeOfPayment)) {
			return null;
		}

		return modeOfPayment.trim().toUpperCase();
	}

	/**
	 * Validate supported payment modes.
	 */
	private void validateModeOfPayment(String modeOfPayment) {

		String mode = normalizeModeOfPayment(modeOfPayment);

		if (!"CHEQUE".equals(mode) && !"CASH".equals(mode) && !"RTGS".equals(mode) && !"NEFT".equals(mode)) {
			throw new IllegalArgumentException("Invalid Mode Of Payment: " + modeOfPayment + ". Allowed values are CHEQUE, CASH, RTGS and NEFT.");
		}
	}

	private boolean hasValue(String value) {
		return StringUtils.isNotBlank(value);
	}
}