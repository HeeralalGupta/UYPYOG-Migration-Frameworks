package org.egov.finance.migration.common.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

@Service
public class AccountDetailTypeServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeServiceUrl;

	public AccountDetailTypeServiceClient(RestTemplate restTemplate) {

		this.restTemplate = restTemplate;
	}

	/**
	 * Fetch Account Detail Type by name.
	 *
	 * Example:
	 *
	 * Employee
	 *
	 * Returns:
	 *
	 * { "id": 12, "name": "Employee" }
	 */
//	public Accountdetailtype getByName(String name, RequestInfo requestInfo, String tenantId) {
//
//		validate(name, requestInfo, tenantId);
//
//		String url = financeServiceUrl + "/rest/accountdetailtype/v1/_search?tenantId=" + tenantId;
//
//		AccountDetailTypeSearchRequest request = new AccountDetailTypeSearchRequest();
//
//		request.setRequestInfo(requestInfo);
//		request.setTenantId(tenantId);
//		request.setName(name.trim());
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//
//		HttpEntity<AccountDetailTypeSearchRequest> entity = new HttpEntity<>(request, headers);
//		System.out.println("====================================");
//		System.out.println("Sub Ledger Type SEARCH API CALL");
//		System.out.println("URL : " + url);
//		System.out.println("SubLedger Type Name : " + name);
//		System.out.println("Tenant : " + tenantId);
//		System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null
//				&& !requestInfo.getAuthToken().trim().isEmpty()));
//		System.out.println("====================================");
//
//		try {
//
//			ResponseEntity<Accountdetailtype> response = restTemplate.exchange(url, HttpMethod.POST, entity,
//					new ParameterizedTypeReference<Accountdetailtype>() {
//					});
//
//			Accountdetailtype accountDetailType = response.getBody();
//
//			if (accountDetailType == null || accountDetailType.getId() == null) {
//				throw new IllegalArgumentException("SubLedger Type not found: " + name);
//			}
//
//			return accountDetailType;
//
//		} catch (Exception exception) {
//			throw new RuntimeException("Failed to fetch SubLedger Type: " + name, exception);
//		}
//	}

	public Accountdetailtype getByName(String name, RequestInfo requestInfo, String tenantId) {

		validate(name, requestInfo, tenantId);

		String encodedTenantId = URLEncoder.encode(tenantId.trim(), StandardCharsets.UTF_8);
		String encodedAuthToken = URLEncoder.encode(requestInfo.getAuthToken().trim(), StandardCharsets.UTF_8);
		String url = financeServiceUrl + "/rest/accountdetailtype/v1/_search" + "?tenantId=" + encodedTenantId + "&auth_token=" + encodedAuthToken;

		AccountDetailTypeSearchRequest request = new AccountDetailTypeSearchRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);

		// IMPORTANT:
		// API expects accountDetailTypes, NOT name
		request.setAccountDetailTypes(name.trim());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<AccountDetailTypeSearchRequest> entity = new HttpEntity<>(request, headers);

		System.out.println("====================================");
		System.out.println("Sub Ledger Type SEARCH API CALL");
		System.out.println("URL : " + url);
		System.out.println("SubLedger Type Name : " + name);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null
				&& !requestInfo.getAuthToken().trim().isEmpty()));
		System.out.println("====================================");

		try {

			ResponseEntity<Accountdetailtype> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					new ParameterizedTypeReference<Accountdetailtype>() {
					});

			Accountdetailtype accountDetailType = response.getBody();
			System.out.println("====================================================");
			System.out.println("SubLegder Type :"+ accountDetailType.getId()+" SubLegder Type Name :"+ accountDetailType.getName());
			System.out.println("====================================================");

			if (accountDetailType == null || accountDetailType.getId() == null) {

				throw new IllegalArgumentException("SubLedger Type not found: " + name);
			}

			return accountDetailType;

		} catch (Exception exception) {

			throw new RuntimeException("Failed to fetch SubLedger Type: " + name, exception);
		}
	}

	private void validate(String name, RequestInfo requestInfo, String tenantId) {

		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("SubLedger Type name is required");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is required");
		}

		if (tenantId == null || tenantId.trim().isEmpty()) {
			throw new IllegalArgumentException("Tenant ID is required");
		}
	}
}