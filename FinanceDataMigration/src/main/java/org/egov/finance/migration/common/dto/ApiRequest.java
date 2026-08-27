package org.egov.finance.migration.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ApiRequest<T> {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    private String tenantId;

    private T request;
}