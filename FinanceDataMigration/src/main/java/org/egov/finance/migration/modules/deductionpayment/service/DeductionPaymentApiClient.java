package org.egov.finance.migration.modules.deductionpayment.service;

import java.util.Arrays;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.deductionpayment.dto.CreateDeductionPaymentRequest;
import org.egov.finance.migration.modules.deductionpayment.dto.DeductionRemittanceRequest;
import org.egov.finance.migration.modules.deductionpayment.response.DeductionPaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DeductionPaymentApiClient {

	private final RestTemplate restTemplate;

	private final AuthenticationService authenticationService;

	@Value("${finance.deduction-payment.create-url}")
	private String deductionPaymentCreateUrl;

	public DeductionPaymentApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	/**
	 * Creates a deduction payment using the Finance REST API.
	 *
	 * Steps: 1. Validate the request. 2. Get tenant-specific authentication token.
	 * 3. Construct the API URL. 4. Set HTTP headers. 5. Call the deduction-payment
	 * API. 6. Validate the API response.
	 */
	public DeductionPaymentResponse create(CreateDeductionPaymentRequest request) {

		// 1. Validate request
		validateRequest(request);

		String tenantId = request.getTenantId();

		// 2. Get authentication token
		String token = authenticationService.getToken(tenantId);

		if (isBlank(token)) {
			throw new IllegalStateException("Authentication token is null or empty for tenant: " + tenantId);
		}

		// 3. Construct URL
		String url = UriComponentsBuilder.fromUriString(deductionPaymentCreateUrl).queryParam("auth_token", token)
				.queryParam("tenantId", tenantId).build().encode().toUriString();

		// 4. Configure headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(token);

		HttpEntity<CreateDeductionPaymentRequest> entity = new HttpEntity<CreateDeductionPaymentRequest>(request,
				headers);

		try {

			// 5. Call API
			ResponseEntity<DeductionPaymentResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					DeductionPaymentResponse.class);

			if (response == null) {
				throw new IllegalStateException("Deduction payment API returned a null HTTP response.");
			}

			DeductionPaymentResponse responseBody = response.getBody();

			if (responseBody == null) {
				throw new IllegalStateException("Deduction payment API returned an empty response body. HTTP status: "
						+ response.getStatusCode());
			}

			if (!responseBody.isSuccess()) {

				StringBuilder errorMessage = new StringBuilder();

				if (responseBody.getMessage() != null && !responseBody.getMessage().trim().isEmpty()) {
					errorMessage.append(responseBody.getMessage());
				}

				if (responseBody.getErrors() != null && !responseBody.getErrors().isEmpty()) {

					if (errorMessage.length() > 0) {
						errorMessage.append(" | ");
					}

					errorMessage.append(String.join("; ", responseBody.getErrors()));
				}

				if (errorMessage.length() == 0) {
					errorMessage.append("Deduction payment creation failed.");
				}

				throw new IllegalStateException(errorMessage.toString());
			}

			return responseBody;

		} catch (HttpStatusCodeException e) {
			String responseBody = e.getResponseBodyAsString();
			throw new IllegalStateException("Deduction payment API returned HTTP " + e.getStatusCode().value()
					+ " for tenant " + tenantId + ". Response: " + responseBody, e);

		} catch (RestClientException e) {
			throw new IllegalStateException("Unable to communicate with the deduction payment API " + "for tenant "
					+ tenantId + ". Reason: " + e.getMessage(), e);
		}
	}

	/**
	 * Validates mandatory fields before making the REST API call.
	 */
	private void validateRequest(CreateDeductionPaymentRequest request) {

		if (request == null) {
			throw new IllegalArgumentException("Deduction payment request cannot be null.");
		}

		if (isBlank(request.getTenantId())) {
			throw new IllegalArgumentException("Tenant ID is required for deduction payment creation.");
		}

		if (isBlank(deductionPaymentCreateUrl)) {
			throw new IllegalStateException("Deduction payment create URL is not configured. "
					+ "Please configure finance.deduction-payment.create-url.");
		}

		DeductionRemittanceRequest payment = request.getDeductionRemittanceRequest();

		if (payment == null) {
			throw new IllegalArgumentException("Deduction remittance request cannot be null.");
		}

	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
