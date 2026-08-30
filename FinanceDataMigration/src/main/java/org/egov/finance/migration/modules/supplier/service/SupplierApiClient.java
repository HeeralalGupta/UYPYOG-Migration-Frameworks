package org.egov.finance.migration.modules.supplier.service;

import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.supplier.dto.CreateSupplierRequest;
import org.egov.finance.migration.modules.supplier.response.SupplierResponse;
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
public class SupplierApiClient {

    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;

    @Value("${finance.supplier.create-url}")
    private String supplierCreateUrl;

    public SupplierApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    public SupplierResponse createSupplier(CreateSupplierRequest request) {

        // 1. Get tenant
        String tenantId = request.getTenantId();

        System.out.println("=================================");
        System.out.println("SUPPLIER CREATE TENANT : " + tenantId);

        // 2. Get authentication token
        String token = authenticationService.getToken(tenantId);

        System.out.println("SUPPLIER CREATE TOKEN : " + token);

        // 3. Null/empty token check
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException(
                    "Authentication token is null/empty for tenant: "
                            + tenantId);
        }

        // 4. Add auth_token and tenantId to URL
        String url = UriComponentsBuilder
                .fromUriString(supplierCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", tenantId)
                .toUriString();

        System.out.println("SUPPLIER CREATE URL : " + url);

        // 5. Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Bearer token
        headers.setBearerAuth(token);

        // 6. Request
        HttpEntity<CreateSupplierRequest> entity =
                new HttpEntity<>(request, headers);

        // 7. Call API
        ResponseEntity<SupplierResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        SupplierResponse.class
                );

        System.out.println(
                "SUPPLIER API STATUS : "
                        + response.getStatusCode());

        System.out.println(
                "SUPPLIER API RESPONSE : "
                        + response.getBody());

        return response.getBody();
    }
}