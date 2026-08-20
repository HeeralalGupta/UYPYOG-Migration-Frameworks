package org.egov.finance.migration.common.dto;

import java.util.List;


import lombok.Data;

@Data
public class UserInfo {
	private static final long serialVersionUID = -6099520777478122089L;

    private Long id;

    private String uuid;

    private String userName;

    private String name;

    private String type;

    private String mobileNumber;

    private String emailId;

    private List<RoleInfo> roles;

    private String tenantId;
}