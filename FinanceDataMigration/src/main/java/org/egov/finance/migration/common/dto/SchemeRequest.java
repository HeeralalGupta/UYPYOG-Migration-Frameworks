package org.egov.finance.migration.common.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SchemeRequest {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    private String tenantId;

    private List<Integer> ids;

    private String fundName;

    private SchemeSearchRequest schemeSerachRequest;

    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    public void setRequestInfo(RequestInfo requestInfo) {
        this.requestInfo = requestInfo;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<Integer> getIds() {
        return ids;
    }

    public void setIds(List<Integer> ids) {
        this.ids = ids;
    }

    public String getFundName() {
        return fundName;
    }

    public void setFundName(String fundName) {
        this.fundName = fundName;
    }

    public SchemeSearchRequest getSchemeSerachRequest() {
        return schemeSerachRequest;
    }

    public void setSchemeSerachRequest(
            SchemeSearchRequest schemeSerachRequest) {
        this.schemeSerachRequest =
                schemeSerachRequest;
    }
}