package org.egov.finance.migration.common.util;

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
public class AccountDetailKeyServiceClient {

	private final RestTemplate restTemplate;

	@Value("${finance.local.baseurl}")
	private String financeServiceUrl;

	public AccountDetailKeyServiceClient(RestTemplate restTemplate) {

		this.restTemplate = restTemplate;
	}

	/**
	 * Fetch Account Detail Key using:
	 *
	 * Account Detail Type ID + Account Detail Key Name
	 *
	 * Example:
	 *
	 * typeId = 12 name = Raju Kumar
	 *
	 * Returns:
	 *
	 * { "id": 2, "detailname": "Raju Kumar" }
	 */
	public Accountdetailkey getAccountDetailKey(Integer accountDetailTypeId, String name, RequestInfo requestInfo,
			String tenantId) {

		validate(accountDetailTypeId, name, requestInfo, tenantId);
		
		String url = financeServiceUrl + "/rest/accountdetailkey/v1/_search?tenantId="+tenantId;
		AccountDetailKeySearchRequest request = new AccountDetailKeySearchRequest();

		request.setRequestInfo(requestInfo);
		request.setTenantId(tenantId);
		request.setAccountDetailTypeId(accountDetailTypeId);
		request.setName(name.trim());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<AccountDetailKeySearchRequest> entity = new HttpEntity<>(request, headers);
		
		System.out.println("====================================");
		System.out.println("Sub Ledger Master SEARCH API CALL");
		System.out.println("URL : " + url);
		System.out.println("SubLedger Master Name : " + name);
		System.out.println("Tenant : " + tenantId);
		System.out.println("Token Available : " + (requestInfo != null && requestInfo.getAuthToken() != null
				&& !requestInfo.getAuthToken().trim().isEmpty()));
		System.out.println("====================================");

		try {

			ResponseEntity<Accountdetailkey> response = restTemplate.exchange(url, HttpMethod.POST, entity,	new ParameterizedTypeReference<Accountdetailkey>() {
					});

			Accountdetailkey accountDetailKey = response.getBody();
			
			System.out.println("====================================================");
			System.out.println("SubLegder Master key :"+accountDetailKey.getDetailkey()+" SubLegder Master Name :"+ accountDetailKey.getDetailname());
			System.out.println("====================================================");

			if (accountDetailKey == null || accountDetailKey.getId() == null) {
				throw new IllegalArgumentException("Sub Ledger Master not found: " + name
						+ " for Sub Ledger Master Type ID: " + accountDetailTypeId);
			}

			return accountDetailKey;

		} catch (Exception exception) {
			throw new RuntimeException("Failed to fetch Sub Ledger Master: " + name + " for Sub Ledger Master Type ID: "
					+ accountDetailTypeId, exception);
		}
	}

	private void validate(Integer accountDetailTypeId, String name, RequestInfo requestInfo, String tenantId) {

		if (accountDetailTypeId == null) {
			throw new IllegalArgumentException("Sub Ledger Master Type ID is required");
		}

		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Sub Ledger Master Key name is required");
		}

		if (requestInfo == null) {
			throw new IllegalArgumentException("RequestInfo is required");
		}

		if (tenantId == null || tenantId.trim().isEmpty()) {
			throw new IllegalArgumentException("Tenant ID is required");
		}
	}
}