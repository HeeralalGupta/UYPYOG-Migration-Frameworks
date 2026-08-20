package org.egov.finance.migration.common.dto;

import lombok.Data;

@Data
public class RequestInfo {
    private String apiId;
    private String ver;
    private String ts;
    private String action;
    private String did;
    private String key;
    private String msgId;
    private String requesterId;
    private String authToken;
    private UserInfo userInfo;
}
