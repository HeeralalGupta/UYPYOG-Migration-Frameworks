package org.egov.finance.migration.modules.contractorbill.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.contractorbill.dto.ContractorBillCreateRequest;
import org.egov.finance.migration.modules.contractorbill.response.ContractorBillResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ContractorBillApiClient {

	private final RestTemplate restTemplate;
	private final AuthenticationService authenticationService;

	@Value("${finance.contractor-bill.create-url}")
	private String contractorBillCreateUrl;

	public ContractorBillApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	public ContractorBillResponse createContractorBill(ContractorBillCreateRequest request) {

		/*
		 * ===================================================== GET TOKEN FOR SELECTED
		 * TENANT =====================================================
		 */

		String token = authenticationService.getToken(request.getTenantId());

		/*
		 * ===================================================== HEADERS
		 * =====================================================
		 */

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);

		/*
		 * ===================================================== REQUEST ENTITY
		 * =====================================================
		 */

		HttpEntity<ContractorBillCreateRequest> entity = new HttpEntity<>(request, headers);

		/*
		 * ===================================================== CALL CONTRACTOR BILL
		 * API =====================================================
		 */
		ResponseEntity<String> response = null;
		try {
		response = restTemplate.exchange(contractorBillCreateUrl, HttpMethod.POST, entity,
				String.class);

		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
		/*
		 * ===================================================== LOG STATUS
		 * =====================================================
		 */

		System.out.println("CONTRACTOR BILL API STATUS : " + response.getStatusCode());

		/*
		 * ===================================================== RETURN RESPONSE
		 * =====================================================
		 */
		return null;
//		return response.getBody();
	}
}