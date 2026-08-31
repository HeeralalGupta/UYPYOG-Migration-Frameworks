package org.egov.finance.migration.dashboard.dto;

import java.util.List;

import lombok.Data;

@Data
public class MigrationActivityResponse {

    private List<String> labels;
    private List<Long> successful;
    private List<Long> failed;
    private List<Long> running;
}