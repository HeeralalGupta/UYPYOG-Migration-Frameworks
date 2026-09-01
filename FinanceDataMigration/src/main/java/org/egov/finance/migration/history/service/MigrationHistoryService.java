package org.egov.finance.migration.history.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.config.TenantConfig;
import org.egov.finance.migration.history.dto.MigrationHistoryJob;
import org.egov.finance.migration.history.dto.MigrationHistoryOptionsResponse;
import org.egov.finance.migration.history.dto.MigrationHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class MigrationHistoryService {

	private final MigrationJobRepository migrationJobRepository;
	private final TenantConfig tenantConfig;

	public MigrationHistoryService(MigrationJobRepository migrationJobRepository, TenantConfig tenantConfig) {

		this.migrationJobRepository = migrationJobRepository;
		this.tenantConfig = tenantConfig;
	}

	public MigrationHistoryResponse search(String jobId, String module, String tenant, String status,
			LocalDateTime fromDate, LocalDateTime toDate, int page, int pageSize) {

		/*
		 * ===================================================== NORMALIZE PAGINATION
		 * =====================================================
		 */

		if (page < 0) {
			page = 0;
		}

		if (pageSize <= 0) {
			pageSize = 10;
		}

		if (pageSize > 100) {
			pageSize = 100;
		}

		/*
		 * ===================================================== LOAD DATA
		 * =====================================================
		 */

		Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "startedTime"));
		Page<MigrationJob> result = migrationJobRepository.findAll(pageable);

		/*
		 * ===================================================== FILTER
		 * =====================================================
		 */

		List<MigrationJob> filtered = result.getContent().stream()
				.filter(job -> matches(job, jobId, module, tenant, status, fromDate, toDate)).toList();

		/*
		 * ===================================================== RESPONSE
		 * =====================================================
		 */

		MigrationHistoryResponse response = new MigrationHistoryResponse();

		response.setJobs(filtered.stream().map(this::convert).toList());
		response.setPage(page);
		response.setPageSize(pageSize);
		response.setTotalPages(Long.valueOf(result.getTotalPages()));

		/*
		 * Summary
		 */
		populateSummary(response);

		return response;
	}

	private boolean matches(MigrationJob job, String jobId, String module, String tenant, String status,
			LocalDateTime fromDate, LocalDateTime toDate) {

		if (jobId != null && !jobId.trim().isEmpty()) {
			if (job.getJobId() == null || !job.getJobId().toLowerCase().contains(jobId.trim().toLowerCase())) {
				return false;
			}
		}

		if (module != null && !module.trim().isEmpty() && !"ALL".equalsIgnoreCase(module)) {
			if (job.getModuleCode() == null || !job.getModuleCode().equalsIgnoreCase(module)) {
				return false;
			}
		}

		if (tenant != null && !tenant.trim().isEmpty()) {
			if (job.getTenantId() == null || !job.getTenantId().toLowerCase().contains(tenant.trim().toLowerCase())) {
				return false;
			}
		}

		if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
			if (job.getStatus() == null || !job.getStatus().equalsIgnoreCase(status)) {
				return false;
			}
		}

		if (job.getStartedTime() != null) {
			if (fromDate != null && job.getStartedTime().isBefore(fromDate)) {
				return false;
			}

			if (toDate != null && job.getStartedTime().isAfter(toDate)) {
				return false;
			}
		}

		return true;
	}

	private MigrationHistoryJob convert(MigrationJob job) {

		MigrationHistoryJob dto = new MigrationHistoryJob();

		dto.setJobId(job.getJobId());
		dto.setModule(job.getModuleCode());
		dto.setTenant(job.getTenantId());
		dto.setStatus(job.getStatus());
		dto.setTotalRecords(job.getTotalRecords());
		dto.setSuccessRecords(job.getSuccessRecords());
		dto.setFailedRecords(job.getFailedRecords());
		dto.setSkippedRecords(job.getSkippedRecords());
		dto.setProgressPercent(job.getProgressPercent());
		dto.setCurrentMessage(job.getCurrentMessage());
		dto.setStartedAt(job.getStartedTime());
		dto.setCompletedAt(job.getCompletedTime());

		return dto;
	}

	private void populateSummary(MigrationHistoryResponse response) {

		List<MigrationHistoryJob> jobs = response.getJobs();

		long successful = jobs.stream().filter(job -> "COMPLETED".equalsIgnoreCase(job.getStatus())).count();
		long failed = jobs.stream().filter(job -> "FAILED".equalsIgnoreCase(job.getStatus())
				|| "COMPLETED_WITH_ERRORS".equalsIgnoreCase(job.getStatus())).count();

		long records = jobs.stream().map(MigrationHistoryJob::getTotalRecords).filter(java.util.Objects::nonNull)
				.mapToLong(Integer::longValue).sum();

		response.setTotalJobs((long) jobs.size());
		response.setSuccessfulJobs(successful);
		response.setFailedJobs(failed);
		response.setTotalRecords(records);
	}

	public MigrationHistoryOptionsResponse getFilterOptions() {

		MigrationHistoryOptionsResponse response = new MigrationHistoryOptionsResponse();

		/*
		 * Modules come from MigrationType enum
		 */
		List<String> modules = Arrays.stream(MigrationType.values()).map(Enum::name).sorted().toList();

		/*
		 * Tenants come from finance.tenants
		 */
		List<String> tenants = tenantConfig.getTenants().stream().map(String::trim).filter(value -> !value.isEmpty())
				.distinct().sorted().toList();

		response.setModules(modules);
		response.setTenants(tenants);

		return response;
	}

	public byte[] exportExcel(String jobId, String module, String tenant, String status, LocalDateTime fromDate,
			LocalDateTime toDate) throws IOException {

		List<MigrationJob> jobs = migrationJobRepository.findAll(Sort.by(Sort.Direction.DESC, "startedTime")).stream()
				.filter(job -> matches(job, jobId, module, tenant, status, fromDate, toDate)).toList();

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Migration History");

			/*
			 * Header
			 */
			Row header = sheet.createRow(0);

			String[] headers = {

					"Job ID", "Module", "Tenant", "Status", "Total Records", "Successful Records", "Failed Records",
					"Skipped Records", "Progress %", "Started", "Completed", "Duration", "Message"

			};

			for (int i = 0; i < headers.length; i++) {
				Cell cell = header.createCell(i);
				cell.setCellValue(headers[i]);

			}

			/*
			 * Data
			 */
			int rowNumber = 1;

			for (MigrationJob job : jobs) {

				Row row = sheet.createRow(rowNumber++);
				row.createCell(0).setCellValue(safe(job.getJobId()));
				row.createCell(1).setCellValue(safe(job.getModuleCode()));
				row.createCell(2).setCellValue(safe(job.getTenantId()));
				row.createCell(3).setCellValue(safe(job.getStatus()));
				row.createCell(4).setCellValue(safeInt(job.getTotalRecords()));
				row.createCell(5).setCellValue(safeInt(job.getSuccessRecords()));
				row.createCell(6).setCellValue(safeInt(job.getFailedRecords()));
				row.createCell(7).setCellValue(safeInt(job.getSkippedRecords()));
				row.createCell(8).setCellValue(safeInt(job.getProgressPercent()));
				row.createCell(9).setCellValue(safeDateTime(job.getStartedTime()));
				row.createCell(10).setCellValue(safeDateTime(job.getCompletedTime()));
				row.createCell(11).setCellValue(calculateDurationForExport(job));
				row.createCell(12).setCellValue(safe(job.getCurrentMessage()));

			}

			/*
			 * Auto-size
			 */
			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);

			}

			workbook.write(outputStream);
			return outputStream.toByteArray();
		}
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private int safeInt(Integer value) {
		return value == null ? 0 : value;
	}

	private String safeDateTime(LocalDateTime value) {
		return value == null ? "" : value.toString();
	}

	private String calculateDurationForExport(MigrationJob job) {
		if (job.getStartedTime() == null) {
			return "";
		}

		LocalDateTime end = job.getCompletedTime() != null ? job.getCompletedTime() : LocalDateTime.now();

		long seconds = java.time.Duration.between(job.getStartedTime(), end).getSeconds();
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long remaining = seconds % 60;
		if (hours > 0) {
			return hours + "h " + minutes + "m " + remaining + "s";
		}

		if (minutes > 0) {
			return minutes + "m " + remaining + "s";
		}

		return remaining + "s";
	}

}