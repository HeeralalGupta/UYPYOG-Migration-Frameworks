package org.egov.finance.migration.config;


import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TenantConfig {

    @Value("${finance.tenants}")
    private String tenants;

    public List<String> getTenants() {
        return Arrays.asList(tenants.split(","));
    }
}
