package org.egov.finance.migration.modules.contractorbill.service;

import java.net.URI;
import java.util.Collections;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.contractorbill.dto.ContractorBillCreateRequest;
import org.egov.finance.migration.modules.contractorbill.response.ContractorBillResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ContractorBillApiClient {

	private static final Logger logger = LoggerFactory.getLogger(ContractorBillApiClient.class);

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.contractor-bill.create-url}")
	private String contractorBillCreateUrl;

	public ContractorBillApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	public ContractorBillResponse createContractorBill(ContractorBillCreateRequest request) {

		String tenantId = request.getTenantId();

		// Get authentication token for the requested tenant
		String token = authenticationService.getToken(tenantId);

		// Build URL with auth_token and tenantId query parameters
		URI uri = UriComponentsBuilder.fromUriString(contractorBillCreateUrl).replaceQueryParam("auth_token", token)
				.replaceQueryParam("tenantId", tenantId).build().encode().toUri();

		// Prepare headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		// Keep Bearer authentication as well
		headers.setBearerAuth(token);

		// Prepare request entity
		HttpEntity<ContractorBillCreateRequest> entity = new HttpEntity<>(request, headers);

		try {

			// Do not log the complete URI because it contains the token
			logger.info("Calling Contractor Bill API. Tenant={}, Method=POST", tenantId);

			ResponseEntity<ContractorBillResponse> response = restTemplate.exchange(uri, HttpMethod.POST, entity,
					ContractorBillResponse.class);

			logger.info("Contractor Bill API Status: {}", response.getStatusCode());

			logger.info("Contractor Bill API completed for tenant={}", tenantId);

			return response.getBody();

		} catch (HttpStatusCodeException e) {

			logger.error("Contractor Bill API HTTP Error. Status={}, Body={}", e.getStatusCode(),
					e.getResponseBodyAsString());

			throw e;

		} catch (RestClientException e) {

			logger.error("Contractor Bill API connection/request failed. Tenant={}", tenantId, e);

			throw e;
		}
	}
}