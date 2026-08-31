package org.egov.finance.migration.common.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.egov.finance.migration.common.dto.Function;
import org.egov.finance.migration.common.dto.FunctionRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FunctionServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeHost;

	@Value("${function.search}")
	private String functionSearch;

	public FunctionServiceClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public Function getFunctionByName(String functionName, RequestInfo requestInfo, String tenantId) {

		/*
		 * ===================================================== VALIDATION
		 * =====================================================
		 */

		if (functionName == null || functionName.trim().isEmpty()) {
			throw new IllegalArgumentException("Function name is empty.");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is required.");
		}

		if (requestInfo.getAuthToken() == null || requestInfo.getAuthToken().trim().isEmpty()) {
			throw new IllegalArgumentException("Auth token is required.");
		}

		if (tenantId == null || tenantId.trim().isEmpty()) {
			throw new IllegalArgumentException("Tenant ID is required.");
		}

		/*
		 * ===================================================== REQUEST BODY
		 * =====================================================
		 */

		FunctionRequest request = new FunctionRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setName(functionName.trim());
		request.setActive(true);
		request.setIds(null);
		request.setPageSize(20);
		request.setOffset(0);
		request.setSortBy("name");

		/*
		 * ===================================================== QUERY PARAMETERS
		 * =====================================================
		 *
		 * Legacy Finance security expects:
		 *
		 * auth_token tenantId
		 *
		 * in query string.
		 */

		String encodedTenantId = URLEncoder.encode(tenantId.trim(), StandardCharsets.UTF_8);
		String encodedAuthToken = URLEncoder.encode(requestInfo.getAuthToken().trim(), StandardCharsets.UTF_8);
		String url = financeHost + functionSearch + "?tenantId=" + encodedTenantId + "&auth_token=" + encodedAuthToken;

		/*
		 * ===================================================== HEADERS
		 * =====================================================
		 */

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<FunctionRequest> entity = new HttpEntity<>(request, headers);

		/*
		 * ===================================================== LOG
		 * =====================================================
		 */

		System.out.println("====================================");
		System.out.println("FUNCTION SEARCH API CALL");
		System.out.println("URL : " + url);
		System.out.println("Function Name : " + functionName);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : "
				+ (requestInfo.getAuthToken() != null && !requestInfo.getAuthToken().trim().isEmpty()));
		System.out.println("====================================");

		/*
		 * ===================================================== API CALL
		 * =====================================================
		 */

		ResponseEntity<FunctionResponse> response;

		try {
			response = restTemplate.exchange(url, HttpMethod.POST, entity, FunctionResponse.class);
		} catch (Exception e) {

			throw new RuntimeException("Failed to search Function: " + functionName, e);
		}

		/*
		 * ===================================================== STATUS
		 * =====================================================
		 */

		System.out.println("FUNCTION API STATUS : " + response.getStatusCode());

		/*
		 * ===================================================== RESPONSE
		 * =====================================================
		 */

		FunctionResponse body = response.getBody();

		if (body == null || body.getFunctions() == null || body.getFunctions().isEmpty()) {
			throw new IllegalArgumentException("Function not found: " + functionName);
		}

		/*
		 * ===================================================== EXACT NAME MATCH
		 * =====================================================
		 *
		 * Search API is LIKE based, so don't blindly take the first result.
		 */

		for (Function function : body.getFunctions()) {

			if (function.getName() != null && function.getName().trim().equalsIgnoreCase(functionName.trim())) {
				if (function.getId() == null) {
					throw new IllegalArgumentException("Function ID not found for function: " + functionName);
				}

				return function;
			}
		}

		/*
		 * ===================================================== EXACT MATCH NOT FOUND
		 * =====================================================
		 */

		throw new IllegalArgumentException("Exact function not found: " + functionName);
	}
}