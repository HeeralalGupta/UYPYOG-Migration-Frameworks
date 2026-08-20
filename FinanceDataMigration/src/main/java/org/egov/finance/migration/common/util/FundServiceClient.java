package org.egov.finance.migration.common.util;

import java.nio.charset.StandardCharsets;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.common.dto.FundRequest;
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
public class FundServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeHost;

	@Value("${fund.search}")
	private String fundSearch;

	public FundServiceClient(RestTemplate restTemplate) {

		this.restTemplate = restTemplate;
	}

	public Fund getFundByName(String fundName, RequestInfo requestInfo, String tenantId) {

		if (fundName == null || fundName.trim().isEmpty()) {
			throw new IllegalArgumentException("Fund name is empty.");
		}

		String token = requestInfo.getAuthToken();

		if (token == null || token.trim().isEmpty()) {
			throw new IllegalArgumentException("Authentication token is missing.");
		}

		FundRequest request = new FundRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setName(fundName.trim());
		request.setActive(true);
		request.setPageSize(20);
		request.setOffset(0);
		request.setSortBy("name");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<FundRequest> entity = new HttpEntity<>(request, headers);

		/*
		 * IMPORTANT
		 *
		 * Finance application's old security layer does NOT read Authorization: Bearer
		 * <token>.
		 *
		 * It reads:
		 *
		 * request.getParameter("auth_token") request.getParameter("tenantId")
		 */

		String url = financeHost + fundSearch + "?auth_token="
				+ UriUtils.encodeQueryParam(token, StandardCharsets.UTF_8) + "&tenantId="
				+ UriUtils.encodeQueryParam(tenantId, StandardCharsets.UTF_8);

		ResponseEntity<FundResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, FundResponse.class);
		FundResponse body = response.getBody();

		if (body == null || body.getFunds() == null || body.getFunds().isEmpty()) {
			throw new IllegalArgumentException("Fund not found: " + fundName);
		}

		/*
		 * API search is LIKE based. Therefore use exact name match.
		 */
		for (Fund fund : body.getFunds()) {
			if (fund.getName() != null && fund.getName().trim().equalsIgnoreCase(fundName.trim())) {
				return fund;
			}
		}

		throw new IllegalArgumentException("Fund not found: " + fundName);
	}
}