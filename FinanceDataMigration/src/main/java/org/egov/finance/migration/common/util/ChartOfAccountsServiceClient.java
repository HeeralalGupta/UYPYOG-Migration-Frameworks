package org.egov.finance.migration.common.util;

import org.egov.finance.migration.common.dto.ChartOfAccountsRequest;
import org.egov.finance.migration.common.dto.ChartOfAccountsResponse;
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
public class ChartOfAccountsServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeServiceUrl;

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

		String url = financeServiceUrl + "/services/EGF/rest/common/v1/getChartAccountCodeByGlCode" + "?tenantId="
				+ tenantId;

		ChartOfAccountsRequest request = new ChartOfAccountsRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setGlcode(glcode.trim());

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<ChartOfAccountsRequest> entity = new HttpEntity<>(request, headers);

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
}