package org.egov.finance.migration.controller;

import java.util.List;

import org.egov.finance.migration.common.dto.MigrationResult;
import org.egov.finance.migration.common.dto.ProgressResponse;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/migration")
public class MigrationApiController {

    @PostMapping("/process")
    public MigrationResult process() {
    	return null;
    }

    @GetMapping("/progress")
    public ProgressResponse progress() {
    	return null;
    }

    @GetMapping("/history")
    public List<MigrationJob> history() {
    	return null;
    }
}
