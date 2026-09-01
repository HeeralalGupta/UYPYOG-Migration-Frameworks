package org.egov.finance.migration.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.egov.finance.migration.config.TenantConfig;
import org.egov.finance.migration.history.dto.MigrationHistoryOptionsResponse;
import org.egov.finance.migration.history.dto.MigrationHistoryResponse;
import org.egov.finance.migration.history.service.MigrationHistoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/migration")
public class MigrationPageController {

	private final TenantConfig tenantConfig;
	private final MigrationHistoryService migrationHistoryService;

	public MigrationPageController(TenantConfig tenantConfig, MigrationHistoryService migrationHistoryService) {
		this.tenantConfig = tenantConfig;
		this.migrationHistoryService = migrationHistoryService;
	}

//    @GetMapping({"", "/"})
//    public String tenantSelection() {
//
//        return "migration/tenant-selection";
//    }

	@GetMapping({ "", "/" })
	public String home() {
		return "migration/home";
	}

	@GetMapping("/new")
	public String newMigration() {
		return "migration/module-selection";
	}

	@GetMapping("/upload/{module}")
	public String upload(@PathVariable String module, Model model) {

		model.addAttribute("tenants", tenantConfig.getTenants());
		model.addAttribute("moduleCode", module);

		return "migration/upload";
	}

	@GetMapping("/reports")
	public String reports() {
		return "migration/reports";
	}

	@GetMapping("/history")
	public String history() {
		return "migration/history";
	}

	@GetMapping("/settings")
	public String settings() {
		return "migration/settings";
	}

	@GetMapping("/history/data")
	public ResponseEntity<MigrationHistoryResponse> historyData(@RequestParam(required = false) String jobId,
			@RequestParam(required = false) String module, @RequestParam(required = false) String tenant,
			@RequestParam(required = false) String status, @RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int pageSize) {
		LocalDateTime from = parseFromDate(fromDate);
		LocalDateTime to = parseToDate(toDate);
		return ResponseEntity
				.ok(migrationHistoryService.search(jobId, module, tenant, status, from, to, page, pageSize));
	}

	@GetMapping("/history/options")
	@ResponseBody
	public ResponseEntity<MigrationHistoryOptionsResponse> getHistoryOptions() {
		return ResponseEntity.ok(migrationHistoryService.getFilterOptions());
	}

	@GetMapping("/history/export")
	public ResponseEntity<byte[]> exportHistory(

			@RequestParam(required = false) String jobId,
			@RequestParam(required = false) String module,
			@RequestParam(required = false) String tenant,
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String fromDate,
			@RequestParam(required = false) String toDate) throws IOException {

		LocalDateTime from = parseFromDate(fromDate);
		LocalDateTime to = parseToDate(toDate);

		byte[] file = migrationHistoryService.exportExcel(jobId, module, tenant, status, from, to);

		String filename = "migration-history-" + LocalDate.now() + ".xlsx";
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.contentType(
						MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(file);
	}

	private LocalDateTime parseFromDate(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return LocalDate.parse(value).atStartOfDay();
	}

	private LocalDateTime parseToDate(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}
		return LocalDate.parse(value).atTime(LocalTime.MAX);
	}

}
