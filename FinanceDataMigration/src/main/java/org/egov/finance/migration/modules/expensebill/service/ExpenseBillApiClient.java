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


	@Value("${finance.local.baseurl}")
	private String financeServiceUrl;
	
	@Value("${finance.expense.bill.create-url}")
	private String expenseBillCreateUrl;

	public ExpenseBillApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	/**
	 * Create Expense Bill.
	 */
//	public ExpenseBillResponse createExpenseBill(ExpenseBillCreateRequest request) {
//
////      Get authentication token for tenant.
//		String token = authenticationService.getToken(request.getTenantId());
//
////		Request Headers
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		headers.setBearerAuth(token);
//
////		API Request
//		HttpEntity<ExpenseBillCreateRequest> entity = new HttpEntity<>(request, headers);
//
////		Call Expense Bill Create API
//		ResponseEntity<ExpenseBillResponse> response = restTemplate.exchange(expenseBillCreateUrl, HttpMethod.POST,
//				entity, ExpenseBillResponse.class);
//		System.out.println("EXPENSE BILL API STATUS : " + response.getStatusCode());
//		return response.getBody();
//	}

	/**
	 * Create Expense Bill.
	 */
	public ExpenseBillResponse createExpenseBill(ExpenseBillCreateRequest request) {

		// Get authentication token for tenant
		String token = authenticationService.getToken(request.getTenantId());

		// IMPORTANT:
		// Set token inside RequestInfo because legacy EGF APIs
		// use RequestInfo.authToken
		if (request.getRequestInfo() != null) {
			request.getRequestInfo().setAuthToken(token);
		}

		// Ensure tenant ID exists
		String tenantId = request.getTenantId();

		// Build URL exactly like Postman
		String url = financeServiceUrl+expenseBillCreateUrl;

		if (!url.contains("tenantId=")) {
			url = url + "?tenantId=" + tenantId;
		}

		// Request Headers
		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);

		// Optional Bearer authentication
		headers.setBearerAuth(token);

		// API Request
		HttpEntity<ExpenseBillCreateRequest> entity = new HttpEntity<>(request, headers);

		System.out.println("====================================");
		System.out.println("EXPENSE BILL CREATE API CALL");
		System.out.println("URL : " + url);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + (token != null && !token.trim().isEmpty()));
		System.out.println("====================================");

		try {

			ResponseEntity<ExpenseBillResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					ExpenseBillResponse.class);

			System.out.println("EXPENSE BILL API STATUS : " + response.getStatusCode());

			return response.getBody();

		} catch (Exception exception) {

			throw new RuntimeException("Failed to create Expense Bill for tenant: " + tenantId, exception);
		}
	}

}