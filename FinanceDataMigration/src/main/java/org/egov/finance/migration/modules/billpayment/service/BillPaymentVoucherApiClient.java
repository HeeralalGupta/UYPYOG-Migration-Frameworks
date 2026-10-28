package org.egov.finance.migration.modules.billpayment.service;

import java.net.URI;
import java.util.Collections;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.billpayment.dto.PaymentCreateRequest;
import org.egov.finance.migration.modules.billpayment.response.BillPaymentVoucherResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BillPaymentVoucherApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.payment.create-url}")
	private String paymentCreateUrl;

	public BillPaymentVoucherApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {
		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	public BillPaymentVoucherResponse createPayment(ApiRequest<PaymentCreateRequest> apiRequest, String tenantId) {

		String token = authenticationService.getToken(tenantId);

		URI uri = UriComponentsBuilder.fromUriString(paymentCreateUrl).replaceQueryParam("auth_token", token)
				.replaceQueryParam("tenantId", tenantId).build().encode().toUri();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		headers.setBearerAuth(token);

		HttpEntity<ApiRequest<PaymentCreateRequest>> entity = new HttpEntity<>(apiRequest, headers);

		ResponseEntity<BillPaymentVoucherResponse> response = restTemplate.exchange(uri, HttpMethod.POST, entity,
				BillPaymentVoucherResponse.class);

		return response.getBody();
	}
}
