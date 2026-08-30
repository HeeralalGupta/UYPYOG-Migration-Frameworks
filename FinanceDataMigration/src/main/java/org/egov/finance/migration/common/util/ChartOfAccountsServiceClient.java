package org.egov.finance.migration.common.util;

import java.nio.charset.StandardCharsets;

import org.egov.finance.migration.common.dto.ChartOfAccountsResponse;
import org.egov.finance.migration.common.dto.ChartOfAccountsSearchRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

@Service
public class ChartOfAccountsServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeServiceUrl;

	@Value("${glcode.search}")
	private String glcodeSearch;

	public ChartOfAccountsServiceClient(RestTemplate restTemplate) {

		this.restTemplate = restTemplate;
	}

	/**
	 * Fetch Chart Of Account by GL Code.
	 *
	 * Example:
	 *
	 * 2101000001
	 *
	 * Returns:
	 *
	 * { "id": 438, "glcode": "2101000001", "name": "Salaries and Allowances —
	 * Officers" }
	 */
	public ChartOfAccountsResponse getByGlCode(String glcode, RequestInfo requestInfo, String tenantId) {

		validate(glcode, requestInfo, tenantId);

		String url = financeServiceUrl + glcodeSearch + "?tenantId="
				+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);

		ChartOfAccountsSearchRequest request = new ChartOfAccountsSearchRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setGlcode(glcode.trim());

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<ChartOfAccountsSearchRequest> entity = new HttpEntity<>(request, headers);

		System.out.println("====================================");
		System.out.println("GL Code SEARCH API CALL");
		System.out.println("URL : " + url);
		System.out.println("COA  GL Code : " + glcode);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null
				&& !requestInfo.getAuthToken().trim().isEmpty()));
		System.out.println("====================================");

		try {

			ResponseEntity<ChartOfAccountsResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					new ParameterizedTypeReference<ChartOfAccountsResponse>() {
					});

			ChartOfAccountsResponse chartOfAccounts = response.getBody();

			if (chartOfAccounts == null || chartOfAccounts.getId() == null) {
				throw new IllegalArgumentException("GL Code not found: " + glcode);
			}
			return chartOfAccounts;
		} catch (Exception exception) {
			throw new RuntimeException("Failed to fetch Chart Of Account for GL Code: " + glcode, exception);
		}
	}

	private void validate(String glcode, RequestInfo requestInfo, String tenantId) {

		if (glcode == null || glcode.trim().isEmpty()) {
			throw new IllegalArgumentException("GL Code is required");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is required");
		}

		if (tenantId == null || tenantId.trim().isEmpty()) {
			throw new IllegalArgumentException("Tenant ID is required");
		}
	}

//	public ChartOfAccounts getByGlCode(String glCode, RequestInfo requestInfo, String tenantId) {
//
//		try {
//
//			String url = financeHost + chartOfAccountsSearch + "?tenantId="
//					+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);
//
//			System.out.println("====================================");
//			System.out.println("GL Code SEARCH API CALL");
//			System.out.println("URL : " + url);
//			System.out.println("COA GL Code : " + glCode);
//			System.out.println("Tenant : " + tenantId);
//			System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null));
//			System.out.println("====================================");
//
//			ChartOfAccountsSearchRequest request = new ChartOfAccountsSearchRequest();
//
//			request.setGlcode(glCode);
//			request.setRequestInfo(requestInfo);
//			request.setTenantId(tenantId);
//
//			HttpHeaders headers = new HttpHeaders();
//
//			headers.setContentType(MediaType.APPLICATION_JSON);
//
//			/*
//			 * IMPORTANT: Legacy EGF RestServiceAuthFilter may read token from HTTP header.
//			 */
//			if (requestInfo != null && requestInfo.getAuthToken() != null
//					&& !requestInfo.getAuthToken().trim().isEmpty()) {
//
//				headers.set("authToken", requestInfo.getAuthToken());
//			}
//
//			HttpEntity<ChartOfAccountsSearchRequest> entity = new HttpEntity<>(request, headers);
//
//			ResponseEntity<ChartOfAccountsResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
//					ChartOfAccountsResponse.class);
//
//			System.out.println("GL CODE API STATUS : " + response.getStatusCode());
//
//			ChartOfAccountsResponse body = response.getBody();
//
//			if (body == null) {
//
//				throw new IllegalArgumentException("Empty response received for GL Code: " + glCode);
//			}
//
//			if (body.getChartOfAccounts() == null || body.getChartOfAccounts().isEmpty()) {
//
//				throw new IllegalArgumentException("Chart Of Account not found for GL Code: " + glCode);
//			}
//
//			return body.getChartOfAccounts().get(0);
//
//		} catch (Exception e) {
//
//			throw new RuntimeException("Failed to fetch Chart Of Account for GL Code: " + glCode, e);
//		}
//	}
//	public ChartOfAccounts getByGlCode(String glCode, RequestInfo requestInfo, String tenantId) {
//
//		try {
//
//			String url = financeHost + chartOfAccountsSearch + "?tenantId="
//					+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);
//
//			ChartOfAccountsSearchRequest request = new ChartOfAccountsSearchRequest();
//
//			request.setRequestInfo(requestInfo);
//			request.setTenantId(tenantId);
//			request.setGlcode(glCode);
//
//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.APPLICATION_JSON);
//
//			HttpEntity<ChartOfAccountsSearchRequest> entity = new HttpEntity<>(request, headers);
//
//			System.out.println("====================================");
//			System.out.println("GL CODE SEARCH API CALL");
//			System.out.println("URL : " + url);
//			System.out.println("COA GL Code : " + glCode);
//			System.out.println("Tenant : " + tenantId);
//			System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null));
//			System.out.println("====================================");
//
//			ResponseEntity<ChartOfAccounts> response = restTemplate.exchange(url, HttpMethod.POST, entity,
//					ChartOfAccounts.class);
//
//			System.out.println("GL CODE API STATUS : " + response.getStatusCode());
//
//			if (response.getBody() == null) {
//				throw new IllegalArgumentException("Chart Of Account not found for GL Code: " + glCode);
//			}
//
//			return response.getBody();
//
//		} catch (Exception e) {
//
//			throw new RuntimeException("Failed to fetch Chart Of Account for GL Code: " + glCode, e);
//		}
//	}
}