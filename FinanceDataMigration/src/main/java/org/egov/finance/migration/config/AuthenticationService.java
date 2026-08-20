package org.egov.finance.migration.config;

import java.time.Instant;

import org.egov.finance.migration.common.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthenticationService {

	private final RestTemplate restTemplate;

	@Value("${finance.auth.token-url}")
	private String tokenUrl;

	@Value("${finance.auth.authorization-key}")
	private String authorizationKey;

	@Value("${finance.auth.username}")
	private String username;

	@Value("${finance.auth.password}")
	private String password;

	@Value("${finance.auth.grant-type}")
	private String grantType;

	@Value("${finance.auth.scope}")
	private String scope;

	@Value("${finance.tenant}")
	private String tenantId;

	private String accessToken;

	private Instant expiryTime;

	public AuthenticationService(RestTemplate restTemplate) {

		this.restTemplate = restTemplate;
	}

	public synchronized String getToken() {

		/*
		 * Reuse existing token if it is still valid.
		 */
		if (accessToken != null && expiryTime != null && Instant.now().isBefore(expiryTime.minusSeconds(60))) {

			return accessToken;
		}

		System.out.println("Generating new Finance OAuth token...");

		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		headers.set("Authorization", authorizationKey);

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

		body.add("username", username);
		body.add("password", password);
		body.add("grant_type", grantType);
		body.add("scope", scope);
		body.add("tenantId", tenantId);
		body.add("userType", "SYSTEM");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

		ResponseEntity<TokenResponse> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request,
				TokenResponse.class);

		TokenResponse token = response.getBody();

		if (token == null || token.getAccessToken() == null || token.getAccessToken().trim().isEmpty()) {

			throw new IllegalStateException("Finance authentication failed: access token is missing.");
		}

		accessToken = token.getAccessToken();

		expiryTime = Instant.now().plusSeconds(token.getExpiresIn());

		System.out.println("Finance OAuth token generated successfully.");

		return accessToken;
	}
}
