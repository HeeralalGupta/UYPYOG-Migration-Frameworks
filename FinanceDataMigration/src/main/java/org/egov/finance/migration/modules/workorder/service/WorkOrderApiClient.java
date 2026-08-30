package org.egov.finance.migration.modules.workorder.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.ApiResponse;
import org.egov.finance.migration.common.dto.ErrorRes;
import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderRequest;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderResponse;

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
public class WorkOrderApiClient {

    private final RestTemplate restTemplate;

    private final AuthenticationService authenticationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finance.workorder.create-url}")
    private String workOrderCreateUrl;

    public WorkOrderApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    /**
     * Create Work Order using Work Order API.
     */
    public ApiResponse<WorkOrderResponse> createWorkOrder(
            ApiRequest<WorkOrderRequest> request) {

        String token = authenticationService
                .getToken(request.getTenantId());

        String url = UriComponentsBuilder
                .fromUriString(workOrderCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", request.getTenantId())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ApiRequest<WorkOrderRequest>> entity =
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
     * Parse successful and error responses from Work Order API.
     */
    private ApiResponse<WorkOrderResponse> parseResponse(
            String responseBody,
            ApiRequest<WorkOrderRequest> request) {

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
                                    WorkOrderResponse.class
                            )
            );

        } catch (Exception e) {

            return ApiResponse.error(
                    request.getRequestInfo(),
                    "Unable to parse Work Order API response"
            );
        }
    }
}
