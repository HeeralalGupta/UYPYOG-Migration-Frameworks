package org.egov.finance.migration.common.dto;

import java.util.Date;

import lombok.Data;

@Data
public class AuditDetails {
    private String tenantId;
    private Long createBy;
    private Long lastModifiedBy;
    private Date createdDate;
    private Date lastModifiedDate;
}
