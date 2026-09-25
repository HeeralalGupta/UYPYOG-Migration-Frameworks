package org.egov.finance.migration.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubSchemeRequest {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    private String tenantId;

    private List<Integer> ids;

    private String schemeName;

    private SubSchemeSearchRequest subSchemeSearchRequest;

    
}