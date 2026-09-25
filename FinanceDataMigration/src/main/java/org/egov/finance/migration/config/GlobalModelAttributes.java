package org.egov.finance.migration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributes {

    private final String appVersion;

    public GlobalModelAttributes(@Value("${app.version}") String appVersion) {
        this.appVersion = appVersion;
    }

    @ModelAttribute
    public void addAppVersion(Model model) {
        model.addAttribute("appVersion", appVersion);
    }
}