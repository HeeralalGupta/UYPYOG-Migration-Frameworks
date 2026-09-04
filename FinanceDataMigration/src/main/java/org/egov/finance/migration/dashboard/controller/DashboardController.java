package org.egov.finance.migration.dashboard.controller;

import org.egov.finance.migration.dashboard.dto.DashboardResponse;
import org.egov.finance.migration.dashboard.dto.MigrationActivityResponse;
import org.egov.finance.migration.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migration/dashboard")
public class DashboardController {

	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {

		this.dashboardService = dashboardService;
	}

	@GetMapping
	public ResponseEntity<DashboardResponse> getDashboard(@RequestParam(required = false) String tenantId) {

		return ResponseEntity.ok(dashboardService.getDashboard(tenantId));
	}

	@GetMapping("/activity")
	public ResponseEntity<MigrationActivityResponse> getMigrationActivity(@RequestParam(defaultValue = "7") int days,
			@RequestParam(required = false) String tenantId) {

		return ResponseEntity.ok(dashboardService.getMigrationActivity(days, tenantId));
	}
}