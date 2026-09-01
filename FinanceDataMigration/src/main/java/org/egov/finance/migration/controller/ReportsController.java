package org.egov.finance.migration.controller;

import java.time.LocalDate;

import org.egov.finance.migration.common.dto.ReportsResponse;
import org.egov.finance.migration.service.ReportsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
	public ResponseEntity<ReportsResponse> getReports(@RequestParam(required = false) LocalDate fromDate,
			@RequestParam(required = false) LocalDate toDate, @RequestParam(required = false) String module,
			@RequestParam(required = false) String tenant) {

		return ResponseEntity.ok(reportsService.generateReport(fromDate, toDate, module, tenant));
	}

	@GetMapping("/export")
	public ResponseEntity<byte[]> exportReport(

			@RequestParam(required = false) String fromDate,

			@RequestParam(required = false) String toDate,

			@RequestParam(required = false) String module,

			@RequestParam(required = false) String tenant) {

		try {

			byte[] excel = reportsService.exportExcel(fromDate, toDate, module, tenant);

			String fileName = "migration-report-" + LocalDate.now() + ".xlsx";

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
					.contentType(MediaType
							.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
					.body(excel);

		} catch (Exception e) {

			throw new RuntimeException("Unable to export migration report", e);
		}
	}

}