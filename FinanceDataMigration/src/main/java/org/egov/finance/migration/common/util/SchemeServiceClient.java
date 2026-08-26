package org.egov.finance.migration.common.util;

import java.util.ArrayList;

import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.Scheme;
import org.egov.finance.migration.common.dto.SchemeRequest;
import org.egov.finance.migration.common.dto.SchemeResponse;
import org.egov.finance.migration.common.dto.SchemeSearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SchemeServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeHost;

	@Value("${scheme.search}")
	private String schemeSearch;

	public SchemeServiceClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public Scheme getSchemeByName(String schemeName, String fundName, RequestInfo requestInfo, String tenantId) {

		/*
		 * ===================================================== VALIDATION
		 * =====================================================
		 */

		if (schemeName == null || schemeName.trim().isEmpty()) {
			throw new IllegalArgumentException("Scheme name is empty.");
		}
		if (fundName == null || fundName.trim().isEmpty()) {
			throw new IllegalArgumentException("Fund name is empty while searching scheme.");
		}

		/*
		 * ===================================================== SCHEME SEARCH REQUEST
		 * =====================================================
		 */

		SchemeRequest request = new SchemeRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setIds(new ArrayList<Integer>());

		/*
		 * Finance controller uses:
		 *
		 * fundService.findByName(schemeRequest.getFundName())
		 */
		request.setFundName(fundName.trim());

		/*
		 * Nested scheme search request
		 */

		SchemeSearchRequest searchRequest = new SchemeSearchRequest();

		searchRequest.setName(schemeName.trim());
		searchRequest.setCode(null);

		/*
		 * We do NOT set fund object here because the Finance controller itself resolves
		 * the fund using fundName.
		 */
		searchRequest.setFund(null);
		request.setSchemeSerachRequest(searchRequest);

		/*
		 * ===================================================== HTTP REQUEST
		 * =====================================================
		 */

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<SchemeRequest> entity = new HttpEntity<>(request, headers);

		/*
		 * ===================================================== LOGGING
		 * =====================================================
		 */

		System.out.println("====================================");
		System.out.println("SCHEME SEARCH API CALL");
		System.out.println("URL : " + financeHost + schemeSearch);
		System.out.println("Scheme Name : " + schemeName);
		System.out.println("Fund Name : " + fundName);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null
				&& !requestInfo.getAuthToken().trim().isEmpty()));
		System.out.println("====================================");

		/*
		 * ===================================================== CALL FINANCE API
		 * =====================================================
		 */

		ResponseEntity<SchemeResponse> response = restTemplate.exchange(financeHost + schemeSearch, HttpMethod.POST,
				entity, SchemeResponse.class);
		System.out.println("SCHEME API STATUS : " + response.getStatusCode());

		/*
		 * ===================================================== RESPONSE
		 * =====================================================
		 */

		SchemeResponse body = response.getBody();

		if (body == null || body.getSchemes() == null || body.getSchemes().isEmpty()) {
			throw new IllegalArgumentException("Scheme not found: " + schemeName + " for fund: " + fundName);
		}

		/*
		 * ===================================================== EXACT NAME MATCH
		 * =====================================================
		 *
		 * Scheme search can be LIKE based. Therefore do not blindly use first result.
		 */

		for (Scheme scheme : body.getSchemes()) {
			if (scheme.getName() != null && scheme.getName().trim().equalsIgnoreCase(schemeName.trim())) {
				return scheme;
			}
		}

		/*
		 * Exact name was not found.
		 */

		throw new IllegalArgumentException("Exact scheme not found: " + schemeName + " for fund: " + fundName);
	}
}
