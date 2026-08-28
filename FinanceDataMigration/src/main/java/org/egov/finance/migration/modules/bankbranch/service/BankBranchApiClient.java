package org.egov.finance.migration.modules.bankbranch.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.bankbranch.dto.CreateBankBranchRequest;
import org.egov.finance.migration.modules.bankbranch.response.BankBranchResponse;
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
public class BankBranchApiClient {

    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;

    @Value("${finance.bankbranch.create-url}")
    private String bankBranchCreateUrl;

    public BankBranchApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    public BankBranchResponse createBankBranch(
            CreateBankBranchRequest request) {

        // 1. Get tenant
        String tenantId = request.getTenantId();

        System.out.println("=================================");
        System.out.println("BANK BRANCH CREATE TENANT : " + tenantId);

        // 2. Get authentication token
        String token = authenticationService.getToken(tenantId);

        System.out.println("BANK BRANCH CREATE TOKEN : " + token);

        // 3. Null/empty token check
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException(
                    "Authentication token is null/empty for tenant: "
                            + tenantId);
        }

        // 4. Add auth_token and tenantId to URL
        String url = UriComponentsBuilder
                .fromUriString(bankBranchCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", tenantId)
                .toUriString();

        System.out.println("BANK BRANCH CREATE URL : " + url);

        // 5. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Keep Bearer token also
        headers.setBearerAuth(token);

        // 6. Request
        HttpEntity<CreateBankBranchRequest> entity =
                new HttpEntity<>(request, headers);

        // 7. Call API
        ResponseEntity<BankBranchResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        BankBranchResponse.class
                );

        System.out.println("BANK BRANCH API STATUS : "
                + response.getStatusCode());

        System.out.println("BANK BRANCH API RESPONSE : "
                + response.getBody());

        return response.getBody();
    }
}