
package org.egov.finance.migration.modules.banktobank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.ApiResponse;
import org.egov.finance.migration.common.dto.ErrorRes;
import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferRequest;
import org.egov.finance.migration.modules.banktobank.dto.BankToBankTransferResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class BankToBankTransferApiClient {

	private final RestTemplate restTemplate;

	private final AuthenticationService authenticationService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${finance.bank-to-bank-transfer.create-url}")
	private String bankToBankTransferCreateUrl;

	public BankToBankTransferApiClient(RestTemplate restTemplate, AuthenticationService authenticationService) {

		this.restTemplate = restTemplate;
		this.authenticationService = authenticationService;
	}

	/**
	 * Create Bank To Bank Transfer.
	 */
	public ApiResponse<BankToBankTransferResponse> createBankToBankTransfer(
			ApiRequest<BankToBankTransferRequest> request) {

		String token = authenticationService.getToken(request.getTenantId());

		String url = UriComponentsBuilder.fromUriString(bankToBankTransferCreateUrl).queryParam("auth_token", token)
				.queryParam("tenantId", request.getTenantId()).toUriString();

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<ApiRequest<BankToBankTransferRequest>> entity = new HttpEntity<>(request, headers);

		try {

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

			String responseBody = response.getBody();

			return parseResponse(responseBody, request);

		} catch (HttpStatusCodeException e) {

			String responseBody = e.getResponseBodyAsString();

			return parseResponse(responseBody, request);
		}
	}

	/**
	 * Parse Bank To Bank Transfer API response.
	 */
	private ApiResponse<BankToBankTransferResponse> parseResponse(String responseBody,
			ApiRequest<BankToBankTransferRequest> request) {

		try {

			JsonNode root = objectMapper.readTree(responseBody);

			if (root.has("Errors")) {

				ErrorRes errorRes = objectMapper.readValue(responseBody, ErrorRes.class);

				return ApiResponse.error(errorRes.getResponseInfo(), errorRes.getErrors());
			}

			return objectMapper.readValue(responseBody, objectMapper.getTypeFactory()
					.constructParametricType(ApiResponse.class, BankToBankTransferResponse.class));

		} catch (Exception e) {

			return ApiResponse.error(request.getRequestInfo(), "Unable to parse Bank To Bank Transfer API response");
		}
	}
}
