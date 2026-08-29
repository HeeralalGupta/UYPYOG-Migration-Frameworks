package org.egov.finance.migration.modules.purchaseorder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.ApiResponse;
import org.egov.finance.migration.common.dto.ErrorRes;
import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRequest;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderResponse;

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
public class PurchaseOrderApiClient {

    private final RestTemplate restTemplate;

    private final AuthenticationService authenticationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finance.purchaseorder.create-url}")
    private String purchaseOrderCreateUrl;

    public PurchaseOrderApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    /**
     * Create Purchase Order using Purchase Order API.
     */
    public ApiResponse<PurchaseOrderResponse> createPurchaseOrder(
            ApiRequest<PurchaseOrderRequest> request) {

        String token = authenticationService
                .getToken(request.getTenantId());

        String url = UriComponentsBuilder
                .fromUriString(purchaseOrderCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", request.getTenantId())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ApiRequest<PurchaseOrderRequest>> entity =
                new HttpEntity<>(request, headers);

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            String responseBody =
                    response.getBody();

            return parseResponse(
                    responseBody,
                    request
            );

        } catch (HttpStatusCodeException e) {

            String responseBody =
                    e.getResponseBodyAsString();

            return parseResponse(
                    responseBody,
                    request
            );
        }
    }

    /**
     * Parse successful and error responses from Purchase Order API.
     */
    private ApiResponse<PurchaseOrderResponse> parseResponse(
            String responseBody,
            ApiRequest<PurchaseOrderRequest> request) {

        try {

            JsonNode root =
                    objectMapper.readTree(responseBody);

            /*
             * Handle API error response.
             */
            if (root.has("Errors")) {

                ErrorRes errorRes =
                        objectMapper.readValue(
                                responseBody,
                                ErrorRes.class
                        );

                return ApiResponse.error(
                        errorRes.getResponseInfo(),
                        errorRes.getErrors()
                );
            }

            /*
             * Handle successful response.
             */
            return objectMapper.readValue(
                    responseBody,
                    objectMapper.getTypeFactory()
                            .constructParametricType(
                                    ApiResponse.class,
                                    PurchaseOrderResponse.class
                            )
            );

        } catch (Exception e) {

            return ApiResponse.error(
                    request.getRequestInfo(),
                    "Unable to parse Purchase Order API response"
            );
        }
    }
}