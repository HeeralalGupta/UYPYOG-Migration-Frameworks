package org.egov.finance.migration.modules.contractor.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.contractor.dto.CreateContractorRequest;
import org.egov.finance.migration.modules.contractor.response.ContractorResponse;
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
public class ContractorApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.contractor.create-url}")
	private String contractorCreateUrl;

	public ContractorApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	public ContractorResponse createContractor(CreateContractorRequest request) {

		// 1. Get tenant
		String tenantId = request.getTenantId();

		System.out.println("=================================");
		System.out.println("CONTRACTOR CREATE TENANT : " + tenantId);

		// 2. Get authentication token
		String token = authenticationService.getToken(tenantId);

		System.out.println("CONTRACTOR CREATE TOKEN : " + token);

		// 3. Null/empty token check
		if (token == null || token.trim().isEmpty()) {
			throw new RuntimeException("Authentication token is null/empty for tenant: " + tenantId);
		}

		// 4. Add auth_token and tenantId to URL
		String url = UriComponentsBuilder.fromUriString(contractorCreateUrl).queryParam("auth_token", token)
				.queryParam("tenantId", tenantId).toUriString();

		System.out.println("CONTRACTOR CREATE URL : " + url);

		// 5. Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Bearer token
		headers.setBearerAuth(token);

		// 6. Request
		HttpEntity<CreateContractorRequest> entity = new HttpEntity<>(request, headers);

		// 7. Call API
		ResponseEntity<ContractorResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
				ContractorResponse.class);

		System.out.println("CONTRACTOR API STATUS : " + response.getStatusCode());

		System.out.println("CONTRACTOR API RESPONSE : " + response.getBody());

		return response.getBody();
	}
}