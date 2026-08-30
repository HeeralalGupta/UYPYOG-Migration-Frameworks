package org.egov.finance.migration.modules.supplier.dto;

import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.Supplier;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateSupplierRequest {

    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("branchName")
    private String branchName;

    @JsonProperty("statusId")
    private Integer statusId;

    @JsonProperty("supplier")
    private Supplier supplier;
}