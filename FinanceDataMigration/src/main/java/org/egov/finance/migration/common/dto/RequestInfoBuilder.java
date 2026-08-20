package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.config.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RequestInfoBuilder {

	private final AuthenticationService authenticationService;

	public RequestInfoBuilder(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public RequestInfo build(String tenantId) {

		RequestInfo requestInfo = new RequestInfo();

		requestInfo.setApiId("Finance Data Migration");
		requestInfo.setAction("CREATE");
		requestInfo.setAuthToken(authenticationService.getToken(tenantId));
		UserInfo userInfo = new UserInfo();
		userInfo.setTenantId(tenantId);
		requestInfo.setUserInfo(userInfo);
		return requestInfo;
	}
}
