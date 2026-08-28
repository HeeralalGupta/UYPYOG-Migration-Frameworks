package org.egov.finance.migration.modules.scheme.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.scheme.dto.CreateSchemeRequest;
import org.egov.finance.migration.modules.scheme.response.SchemeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SchemeApiClient {

    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;

    @Value("${finance.scheme.create-url}")
    private String schemeCreateUrl;

    public SchemeApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    public SchemeResponse createScheme(CreateSchemeRequest request) {

        // 1. Get tenant
        String tenantId = request.getTenantId();

        System.out.println("=================================");
        System.out.println("SCHEME CREATE TENANT : " + tenantId);

        // 2. Get authentication token
        String token = authenticationService.getToken(tenantId);

        System.out.println("SCHEME CREATE TOKEN : " + token);

        // 3. Null/empty token check
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException(
                    "Authentication token is null/empty for tenant: " + tenantId);
        }

        // 4. Add auth_token and tenantId to URL
        String url = UriComponentsBuilder
                .fromUriString(schemeCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", tenantId)
                .toUriString();

        System.out.println("SCHEME CREATE URL : " + url);

        // 5. Headers
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        // Keep Bearer token also
        headers.setBearerAuth(token);

        // 6. Request
        HttpEntity<CreateSchemeRequest> entity =
                new HttpEntity<>(request, headers);

        // 7. Call Scheme API
        ResponseEntity<SchemeResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        SchemeResponse.class
                );

        System.out.println("SCHEME API STATUS : "
                + response.getStatusCode());

        System.out.println("SCHEME API RESPONSE : "
                + response.getBody());

        return response.getBody();
    }
}