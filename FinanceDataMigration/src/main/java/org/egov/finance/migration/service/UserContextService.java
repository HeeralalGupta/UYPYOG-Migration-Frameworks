package org.egov.finance.migration.service;

import jakarta.servlet.http.HttpSession;
import org.egov.finance.migration.common.dto.UserContext;
import org.springframework.stereotype.Service;

@Service
public class UserContextService {

	public static final String SESSION_KEY = "MIGRATION_USER_CONTEXT";

	public void setUserContext(HttpSession session, String username, String tenantId) {
		UserContext userContext = UserContext.builder().username(username).tenantId(tenantId).build();
		session.setAttribute(SESSION_KEY, userContext);
	}

	public UserContext getUserContext(HttpSession session) {
		return (UserContext) session.getAttribute(SESSION_KEY);
	}
}