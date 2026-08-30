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

import tools.jackson.databind.ObjectMapper;

@Service
public class SchemeServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeHost;

	@Value("${scheme.search}")
	private String schemeSearch;
	private final ObjectMapper objectMapper;

	public SchemeServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper) {

		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}

	/*
	 * public Scheme getSchemeByName(String schemeName, String fundName, RequestInfo
	 * requestInfo, String tenantId) {
	 * 
	 * if (schemeName == null || schemeName.trim().isEmpty()) { throw new
	 * IllegalArgumentException("Scheme name is empty."); }
	 * 
	 * if (fundName == null || fundName.trim().isEmpty()) { throw new
	 * IllegalArgumentException("Fund name is empty while searching scheme."); }
	 * 
	 * SchemeRequest request = new SchemeRequest();
	 * 
	 * request.setRequestInfo(requestInfo); request.setTenantId(tenantId);
	 * request.setIds(new ArrayList<Integer>());
	 * request.setFundName(fundName.trim());
	 * 
	 * SchemeSearchRequest searchRequest = new SchemeSearchRequest();
	 * 
	 * searchRequest.setName(schemeName.trim()); searchRequest.setCode(null);
	 * searchRequest.setFund(null);
	 * 
	 * request.setSchemeSerachRequest(searchRequest);
	 * 
	 * HttpHeaders headers = new HttpHeaders();
	 * headers.setContentType(MediaType.APPLICATION_JSON);
	 * 
	 * HttpEntity<SchemeRequest> entity = new HttpEntity<>(request, headers);
	 * 
	 * String url = financeHost + schemeSearch + "?token=" +
	 * UriUtils.encodeQueryParam(requestInfo.getAuthToken(), StandardCharsets.UTF_8)
	 * + "&tenantId=" + UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);
	 * 
	 * System.out.println("====================================");
	 * System.out.println("SCHEME SEARCH API CALL"); System.out.println("URL : " +
	 * url); System.out.println("Scheme Name : " + schemeName);
	 * System.out.println("Fund Name : " + fundName); System.out.println("Tenant : "
	 * + tenantId); System.out.println("====================================");
	 * 
	 * try {
	 * 
	 * ResponseEntity<SchemeResponse> response = restTemplate.exchange(url,
	 * HttpMethod.POST, entity, SchemeResponse.class);
	 * 
	 * System.out.println("SCHEME API STATUS : " + response.getStatusCode());
	 * 
	 * SchemeResponse body = response.getBody();
	 * 
	 * if (body == null || body.getSchemes() == null || body.getSchemes().isEmpty())
	 * {
	 * 
	 * throw new IllegalArgumentException("Scheme not found: " + schemeName +
	 * " for fund: " + fundName); }
	 * 
	 * for (Scheme scheme : body.getSchemes()) {
	 * 
	 * if (scheme.getName() != null &&
	 * scheme.getName().trim().equalsIgnoreCase(schemeName.trim())) {
	 * 
	 * System.out.println("SCHEME FOUND : " + scheme.getName() + ", ID : " +
	 * scheme.getId());
	 * 
	 * return scheme; } }
	 * 
	 * throw new IllegalArgumentException("Exact scheme not found: " + schemeName +
	 * " for fund: " + fundName);
	 * 
	 * } catch (Exception e) {
	 * 
	 * throw new RuntimeException( "Failed while searching scheme. " + "Scheme=" +
	 * schemeName + ", Fund=" + fundName, e); } }
	 */

	public Scheme getSchemeByName(String schemeName, String fundName, RequestInfo requestInfo, String tenantId) {

		if (schemeName == null || schemeName.trim().isEmpty()) {
			throw new IllegalArgumentException("Scheme name is empty.");
		}

		if (fundName == null || fundName.trim().isEmpty()) {
			throw new IllegalArgumentException("Fund name is empty while searching scheme.");
		}

		SchemeRequest request = new SchemeRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setIds(new ArrayList<Integer>());
		request.setFundName(fundName.trim());

		SchemeSearchRequest searchRequest = new SchemeSearchRequest();

		searchRequest.setName(schemeName.trim());
		searchRequest.setCode(null);
		searchRequest.setFund(null);

		request.setSchemeSerachRequest(searchRequest);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<SchemeRequest> entity = new HttpEntity<>(request, headers);

		String url = financeHost + schemeSearch + "?tenantId="
				+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);

		System.out.println("====================================");
		System.out.println("SCHEME SEARCH API CALL");
		System.out.println("URL : " + url);
		System.out.println("Scheme Name : " + schemeName);
		System.out.println("Fund Name : " + fundName);
		System.out.println("Tenant : " + tenantId);
		System.out.println("====================================");

		try {

			ResponseEntity<SchemeResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
					SchemeResponse.class);

			System.out.println("SCHEME API STATUS : " + response.getStatusCode());

			SchemeResponse body = response.getBody();

			if (body == null || body.getSchemes() == null || body.getSchemes().isEmpty()) {

				throw new IllegalArgumentException("Scheme not found: " + schemeName + " for fund: " + fundName);
			}

			for (Scheme scheme : body.getSchemes()) {

				if (scheme.getName() != null && scheme.getName().trim().equalsIgnoreCase(schemeName.trim())) {
					System.out.println("SCHEME FOUND : " + scheme.getName() + ", ID : " + scheme.getId());
					return scheme;
				}
			}

			throw new IllegalArgumentException("Exact scheme not found: " + schemeName + " for fund: " + fundName);

		} catch (Exception e) {

			throw new RuntimeException(
					"Failed while searching scheme. " + "Scheme=" + schemeName + ", Fund=" + fundName, e);
		}
	}

}
