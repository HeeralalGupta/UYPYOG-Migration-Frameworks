package org.egov.finance.migration.modules.work.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.egov.finance.migration.common.dto.ApiRequest;
import org.egov.finance.migration.common.dto.ApiResponse;
import org.egov.finance.migration.common.dto.ErrorRes;
import org.egov.finance.migration.config.AuthenticationService;
import org.egov.finance.migration.modules.work.dto.WorkRequest;
import org.egov.finance.migration.modules.work.dto.WorkResponse;

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
public class WorkApiClient {

    private final RestTemplate restTemplate;

    private final AuthenticationService authenticationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finance.work.create-url}")
    private String workCreateUrl;

    public WorkApiClient(
            RestTemplate restTemplate,
            AuthenticationService authenticationService) {

        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    public ApiResponse<WorkResponse> createWork(
            ApiRequest<WorkRequest> request) {

        String token = authenticationService
                .getToken(request.getTenantId());

        String url = UriComponentsBuilder
                .fromUriString(workCreateUrl)
                .queryParam("auth_token", token)
                .queryParam("tenantId", request.getTenantId())
                .toUriString();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ApiRequest<WorkRequest>> entity =
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

    private ApiResponse<WorkResponse> parseResponse(
            String responseBody,
            ApiRequest<WorkRequest> request) {

        try {

            JsonNode root =
                    objectMapper.readTree(responseBody);

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

            return objectMapper.readValue(
                    responseBody,
                    objectMapper.getTypeFactory()
                            .constructParametricType(
                                    ApiResponse.class,
                                    WorkResponse.class
                            )
            );

        } catch (Exception e) {

            return ApiResponse.error(
                    request.getRequestInfo(),
                    "Unable to parse Work API response"
            );
        }
    }
}