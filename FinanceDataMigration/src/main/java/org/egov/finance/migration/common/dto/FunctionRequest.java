package org.egov.finance.migration.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FunctionRequest {
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    private String tenantId;
    private List<Integer> ids;

    private String name;
  
    private String code;
    private Boolean active;
    private Integer pageSize;
    private Integer offset;
    private String sortBy;
}
