package org.egov.finance.migration.common.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleInfo implements Serializable {

    private static final long serialVersionUID = -1786060370499871338L;

    private Long id;
    private String name;
}
