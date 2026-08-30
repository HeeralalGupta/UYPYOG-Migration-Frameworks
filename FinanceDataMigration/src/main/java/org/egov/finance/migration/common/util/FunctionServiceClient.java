package org.egov.finance.migration.common.util;

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
import org.springframework.web.util.UriUtils;

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

		if (functionName == null || functionName.trim().isEmpty()) {
			throw new IllegalArgumentException("Function name is empty.");
		}

		FunctionRequest request = new FunctionRequest();
		
		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setName(functionName.trim());
		request.setActive(true);
		request.setPageSize(20);
		request.setOffset(0);
		request.setSortBy("name");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<FunctionRequest> entity = new HttpEntity<>(request, headers);

		String url = financeHost + functionSearch + "?token="
				+ UriUtils.encodeQueryParam(requestInfo.getAuthToken(), StandardCharsets.UTF_8)+ "&tenantId="
				+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);
		
		System.out.println("====================================");
		System.out.println("FUNCTION SEARCH API CALL");
		System.out.println("URL : " + url);
		System.out.println("Function Name : " + functionName);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null));
		System.out.println("====================================");

		ResponseEntity<FunctionResponse> response = restTemplate.exchange(url, HttpMethod.POST,
				entity, FunctionResponse.class);

		System.out.println("FUNCTION API STATUS : " + response.getStatusCode());
		FunctionResponse body = response.getBody();

		if (body == null || body.getFunctions() == null || body.getFunctions().isEmpty()) {
			throw new IllegalArgumentException("Function not found: " + functionName);
		}

		/*
		 * API search is LIKE based.
		 *
		 * Therefore do not blindly take the first result. First try exact function
		 * name.
		 */
		for (Function function : body.getFunctions()) {
			if (function.getName() != null && function.getName().trim().equalsIgnoreCase(functionName.trim())) {
				return function;
			}
		}

		/*
		 * Exact function name was not found.
		 */
		throw new IllegalArgumentException("Function not found: " + functionName);
	}
}