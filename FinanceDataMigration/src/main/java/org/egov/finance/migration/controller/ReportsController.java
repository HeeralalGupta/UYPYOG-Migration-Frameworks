package org.egov.finance.migration.controller;

import java.time.LocalDate;

import org.egov.finance.migration.common.dto.ReportsResponse;
import org.egov.finance.migration.service.ReportsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migration/reports")
public class ReportsController {

	private final ReportsService reportsService;

	public ReportsController(ReportsService reportsService) {

		this.reportsService = reportsService;
	}

	@GetMapping("/data")
	public ResponseEntity<ReportsResponse> getReports(
			@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate,
			@RequestParam(required = false) String module,
			@RequestParam(required = false) String tenant) {

		return ResponseEntity.ok(reportsService.generateReport(fromDate, toDate, module, tenant));
	}

}