package org.egov.finance.migration.history.dto;

import java.util.List;

import lombok.Data;

@Data
public class MigrationHistoryOptionsResponse {

    private List<String> modules;

    private List<String> tenants;
}