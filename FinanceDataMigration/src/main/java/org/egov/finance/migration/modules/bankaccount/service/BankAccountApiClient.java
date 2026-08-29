package org.egov.finance.migration.modules.bankaccount.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.bankaccount.dto.CreateBankAccountRequest;
import org.egov.finance.migration.modules.bankaccount.response.BankAccountResponse;
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
public class BankAccountApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.bankaccount.create-url}")
	private String bankAccountCreateUrl;

	public BankAccountApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	public BankAccountResponse createBankAccount(CreateBankAccountRequest request) {

		// 1. Get tenant
		String tenantId = request.getTenantId();
		String branchName = request.getBranchName();
		System.out.println("=================================");
		System.out.println("BANK ACCOUNT CREATE TENANT : " + tenantId);

		// 2. Get authentication token
		String token = authenticationService.getToken(tenantId);

		System.out.println("BANK ACCOUNT CREATE TOKEN : " + token);

		// 3. Null/empty token check
		if (token == null || token.trim().isEmpty()) {
			throw new RuntimeException("Authentication token is null/empty for tenant: " + tenantId);
		}

		// 4. Add auth_token and tenantId to URL
		String url = UriComponentsBuilder.fromUriString(bankAccountCreateUrl).queryParam("auth_token", token)
				.queryParam("tenantId", tenantId).toUriString();

		System.out.println("BANK ACCOUNT CREATE URL : " + url);

		// 5. Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Keep Bearer token also
		headers.setBearerAuth(token);

		// 6. Request
		HttpEntity<CreateBankAccountRequest> entity = new HttpEntity<>(request, headers);

		// 7. Call API
		ResponseEntity<BankAccountResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
				BankAccountResponse.class);

		System.out.println("BANK ACCOUNT API STATUS : " + response.getStatusCode());

		System.out.println("BANK ACCOUNT API RESPONSE : " + response.getBody());

		return response.getBody();
	}
}