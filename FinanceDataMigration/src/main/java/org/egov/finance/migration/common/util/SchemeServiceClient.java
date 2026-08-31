package org.egov.finance.migration.common.util;

import java.nio.charset.StandardCharsets;
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
import org.springframework.web.util.UriUtils;

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

		SchemeRequest request = new SchemeRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);

		/*
		 * Finance controller uses:
		 *
		 * fundService.findByName(schemeRequest.getFundName())
		 */
		request.setFundName(fundName.trim());

		/*
		 * IDs list
		 */
		request.setIds(new ArrayList<Integer>());

		/*
		 * Nested scheme search request
		 */
		SchemeSearchRequest searchRequest = new SchemeSearchRequest();

		searchRequest.setName(schemeName.trim());
		searchRequest.setCode(null);

		/*
		 * Do not set Fund object.
		 *
		 * Finance controller resolves fund using fundName.
		 */
		searchRequest.setFund(null);

		request.setSchemeSerachRequest(searchRequest);

		/*
		 * ===================================================== HTTP HEADERS
		 * =====================================================
		 */

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<SchemeRequest> entity = new HttpEntity<>(request, headers);

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

		String url = financeHost + schemeSearch + "?auth_token="
				+ UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8) + "&tenantId="
				+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);

		System.out.println("SCHEME SEARCH URL : " + url);
		System.out.println("SCHEME NAME       : " + schemeName);
		System.out.println("FUND NAME         : " + fundName);
		System.out.println("TENANT ID         : " + tenantId);

		/*
		 * ===================================================== CALL FINANCE API
		 * =====================================================
		 */

		ResponseEntity<SchemeResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
				SchemeResponse.class);

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
		 * API search may be LIKE based. Therefore do not blindly return first result.
		 */

		for (Scheme scheme : body.getSchemes()) {

			if (scheme.getName() != null && scheme.getName().trim().equalsIgnoreCase(schemeName.trim())) {

				return scheme;
			}
		}

		/*
		 * Exact scheme was not found.
		 */

		throw new IllegalArgumentException("Exact scheme not found: " + schemeName + " for fund: " + fundName);
	}
}