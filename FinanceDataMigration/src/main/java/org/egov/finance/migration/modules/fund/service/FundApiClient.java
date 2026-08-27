package org.egov.finance.migration.modules.fund.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.fund.dto.CreateFundRequest;
import org.egov.finance.migration.modules.fund.response.FundResponse;
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
public class FundApiClient {

    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;

    @Value("${finance.fund.create-url}")
    private String fundCreateUrl;

    public FundApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    public FundResponse createFund(CreateFundRequest request) {

        // 1. Get tenant
        String tenantId = request.getTenantId();

        System.out.println("=================================");
        System.out.println("FUND CREATE TENANT : " + tenantId);

        // 2. Get authentication token
        String token = authenticationService.getToken(tenantId);

        System.out.println("FUND CREATE TOKEN : " + token);

        // 3. Null/empty token check
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException(
                    "Authentication token is null/empty for tenant: " + tenantId);
        }

        // 4. Add auth_token and tenantId to URL
        String url = UriComponentsBuilder
                .fromUriString(fundCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", tenantId)
                .toUriString();

        System.out.println("FUND CREATE URL : " + url);

        // 5. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Keep this also
        headers.setBearerAuth(token);

        // 6. Request
        HttpEntity<CreateFundRequest> entity =
                new HttpEntity<>(request, headers);

        // 7. Call API
        ResponseEntity<FundResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        FundResponse.class
                );

        System.out.println("FUND API STATUS : "
                + response.getStatusCode());

        System.out.println("FUND API RESPONSE : "
                + response.getBody());

        return response.getBody();
    }
}