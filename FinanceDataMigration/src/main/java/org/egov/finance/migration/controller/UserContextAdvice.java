package org.egov.finance.migration.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.egov.finance.migration.common.dto.UserContext;
import org.egov.finance.migration.service.UserContextService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class UserContextAdvice {

    private final UserContextService userContextService;

    @ModelAttribute("userContext")
    public UserContext getUserContext(HttpSession session) {

        return userContextService.getUserContext(session);
    }
}