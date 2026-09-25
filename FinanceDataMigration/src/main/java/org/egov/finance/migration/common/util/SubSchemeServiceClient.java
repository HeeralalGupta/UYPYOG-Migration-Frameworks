package org.egov.finance.migration.common.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.SubScheme;
import org.egov.finance.migration.common.dto.SubSchemeRequest;
import org.egov.finance.migration.common.dto.SubSchemeResponse;
import org.egov.finance.migration.common.dto.SubSchemeSearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@Service
public class SubSchemeServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeHost;

	@Value("${subscheme.search}")
	private String subSchemeSearch;

	public SubSchemeServiceClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public SubScheme getSubSchemeByName(String subSchemeName, String schemeName, RequestInfo requestInfo, String tenantId) {

		/*
		 * ===================================================== VALIDATION
		 * =====================================================
		 */

		if (subSchemeName == null || subSchemeName.trim().isEmpty()) {
			throw new IllegalArgumentException("Sub Scheme name is empty.");
		}

		if (schemeName == null || schemeName.trim().isEmpty()) {
			throw new IllegalArgumentException("Scheme name is empty while searching Sub Scheme.");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is missing.");
		}

		String token = requestInfo.getAuthToken();

		if (token == null || token.trim().isEmpty()) {
			throw new IllegalArgumentException("Authentication token is missing.");
		}

		/*
		 * ===================================================== SCHEME SEARCH REQUEST
		 * =====================================================
		 */

		SubSchemeRequest request = new SubSchemeRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);

		/*
		 * Finance controller uses:
		 *
		 * schemeService.findByName(schemeRequest.getSchemeName())
		 */
		request.setSchemeName(schemeName.trim());

		/*
		 * IDs list
		 */
		request.setIds(new ArrayList<Integer>());

		/*
		 * Nested scheme search request
		 */
		SubSchemeSearchRequest searchRequest = new SubSchemeSearchRequest();

		searchRequest.setName(subSchemeName.trim());
		searchRequest.setCode(null);

		/*
		 * Do not set Fund object.
		 *
		 * Finance controller resolves scheme using schemeName.
		 */
		searchRequest.setScheme(null);

		request.setSubSchemeSearchRequest(searchRequest);

		/*
		 * ===================================================== HTTP HEADERS
		 * =====================================================
		 */

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<SubSchemeRequest> entity = new HttpEntity<>(request, headers);

		/*
		 * ===================================================== AUTHENTICATION
		 * =====================================================
		 *
		 * Old Finance security layer reads:
		 *
		 * request.getParameter("auth_token") request.getParameter("tenantId")
		 *
		 * Therefore token and tenantId must be sent as query parameters.
		 */

		String url = financeHost + subSchemeSearch + "?auth_token="
				+ UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8) + "&tenantId="
				+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);

		System.out.println("SUB SCHEME SEARCH URL : " + url);
		System.out.println("SUB SCHEME NAME       : " + subSchemeName);
		System.out.println("SCHEME NAME       : " + schemeName);
		System.out.println("TENANT ID         : " + tenantId);

		/*
		 * ===================================================== CALL FINANCE API
		 * =====================================================
		 */

		ResponseEntity<SubSchemeResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
				SubSchemeResponse.class);

		System.out.println("SUB SCHEME API STATUS : " + response.getStatusCode());

		/*
		 * ===================================================== RESPONSE
		 * =====================================================
		 */

		SubSchemeResponse body = response.getBody();

		if (body == null || body.getSubSchemes() == null || body.getSubSchemes().isEmpty()) {
			throw new IllegalArgumentException("Sub Scheme not found: " + subSchemeName +" for Scheme : "+schemeName);
		}

		/*
		 * ===================================================== EXACT NAME MATCH
		 * =====================================================
		 *
		 * API search may be LIKE based. Therefore do not blindly return first result.
		 */

		for (SubScheme subScheme : body.getSubSchemes()) {

			if (subScheme.getName() != null && subScheme.getName().trim().equalsIgnoreCase(schemeName.trim())) {

				return subScheme;
			}
		}

		/*
		 * Exact scheme was not found.
		 */

		throw new IllegalArgumentException("Exact sub scheme not found: " + subSchemeName + " for Scheme: " + schemeName);
	}
}