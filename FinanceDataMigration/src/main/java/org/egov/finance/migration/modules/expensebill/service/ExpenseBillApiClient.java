package org.egov.finance.migration.modules.expensebill.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillCreateRequest;
import org.egov.finance.migration.modules.expensebill.response.ExpenseBillResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ExpenseBillApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.expense.bill.create-url}")
	private String expenseBillCreateUrl;

	public ExpenseBillApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	/**
	 * Create Expense Bill.
	 */
	public ExpenseBillResponse createExpenseBill(ExpenseBillCreateRequest request) {

//      Get authentication token for tenant.
		String token = authenticationService.getToken(request.getTenantId());

//		Request Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);

//		API Request
		HttpEntity<ExpenseBillCreateRequest> entity = new HttpEntity<>(request, headers);

//		Call Expense Bill Create API
		ResponseEntity<ExpenseBillResponse> response = restTemplate.exchange(expenseBillCreateUrl, HttpMethod.POST,
				entity, ExpenseBillResponse.class);
		System.out.println("EXPENSE BILL API STATUS : " + response.getStatusCode());
		return response.getBody();
	}
}