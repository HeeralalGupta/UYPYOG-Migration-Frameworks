package org.egov.finance.migration.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.egov.finance.migration.common.dto.ReportsResponse;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportsService {

	private final MigrationJobRepository migrationJobRepository;

	public ReportsService(MigrationJobRepository migrationJobRepository) {

		this.migrationJobRepository = migrationJobRepository;
	}

	public ReportsResponse generateReport(

			LocalDate fromDate, LocalDate toDate, String module, String tenant) {

		/*
		 * ===================================================== LOAD JOBS
		 * =====================================================
		 */

		List<MigrationJob> jobs = migrationJobRepository.findAll();

		/*
		 * ===================================================== FILTER
		 * =====================================================
		 */

		jobs = jobs.stream().filter(job -> matchesDate(job, fromDate, toDate)).filter(job -> matchesModule(job, module))
				.filter(job -> matchesTenant(job, tenant)).collect(Collectors.toList());

		/*
		 * ===================================================== BASIC COUNTS
		 * =====================================================
		 */

		int totalJobs = jobs.size();
		int successfulJobs = countStatus(jobs, "COMPLETED");

		int failedJobs = (int) jobs.stream().filter(job -> "FAILED".equalsIgnoreCase(job.getStatus())
				|| "COMPLETED_WITH_ERRORS".equalsIgnoreCase(job.getStatus())).count();

		int runningJobs = (int) jobs.stream().filter(job -> isRunning(job)).count();
		int totalRecords = jobs.stream().mapToInt(job -> safeInt(job.getTotalRecords())).sum();
		int skippedRecords = jobs.stream().mapToInt(job -> safeInt(job.getSkippedRecords())).sum();

		/*
		 * ===================================================== SUCCESS RATE
		 * =====================================================
		 */

		int completedJobs = (int) jobs.stream().filter(job -> !isRunning(job)).count();

		double successRate = 0;

		if (completedJobs > 0) {

			successRate = ((double) successfulJobs / completedJobs) * 100.0;

		}

		/*
		 * ===================================================== AVERAGE DURATION
		 * =====================================================
		 */

		double averageDuration = calculateAverageDuration(jobs);

		/*
		 * ===================================================== TREND
		 * =====================================================
		 */

		TrendResult trend = buildTrend(jobs);

		/*
		 * ===================================================== MODULE
		 * =====================================================
		 */

		Map<String, Integer> moduleJobs = jobs.stream().filter(job -> job.getModuleCode() != null).collect(Collectors
				.groupingBy(MigrationJob::getModuleCode, LinkedHashMap::new, Collectors.summingInt(job -> 1)));

		/*
		 * ===================================================== TENANT
		 * =====================================================
		 */

		Map<String, Integer> tenantJobs = jobs.stream().filter(job -> job.getTenantId() != null).collect(
				Collectors.groupingBy(MigrationJob::getTenantId, LinkedHashMap::new, Collectors.summingInt(job -> 1)));

		/*
		 * ===================================================== RESPONSE
		 * =====================================================
		 */

		ReportsResponse response = new ReportsResponse();

		response.setTotalJobs(totalJobs);
		response.setSuccessfulJobs(successfulJobs);
		response.setFailedJobs(failedJobs);
		response.setRunningJobs(runningJobs);
		response.setSkippedRecords(skippedRecords);
		response.setTotalRecords(totalRecords);
		response.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
		response.setAverageDurationSeconds(Math.round(averageDuration * 100.0) / 100.0);
		response.setTrendLabels(trend.labels);
		response.setTrendSuccessful(trend.successful);
		response.setTrendFailed(trend.failed);
		response.setTrendRunning(trend.running);
		response.setModuleJobs(moduleJobs);
		response.setTenantJobs(tenantJobs);

		return response;
	}

	private boolean matchesDate(MigrationJob job, LocalDate fromDate, LocalDate toDate) {

		LocalDateTime started = job.getStartedTime();

		if (started == null) {

			return false;

		}

		LocalDate date = started.toLocalDate();

		if (fromDate != null && date.isBefore(fromDate)) {

			return false;

		}

		if (toDate != null && date.isAfter(toDate)) {

			return false;

		}

		return true;
	}

	private boolean matchesModule(MigrationJob job, String module) {

		if (module == null || module.trim().isEmpty() || "ALL".equalsIgnoreCase(module)) {
			return true;

		}

		return module.equalsIgnoreCase(job.getModuleCode());
	}

	private boolean matchesTenant(MigrationJob job, String tenant) {

		if (tenant == null || tenant.trim().isEmpty() || "ALL".equalsIgnoreCase(tenant)) {
			return true;

		}
		return tenant.equalsIgnoreCase(job.getTenantId());
	}

	private boolean isRunning(MigrationJob job) {

		if (job.getStatus() == null) {
			return false;
		}

		String status = job.getStatus().trim().toUpperCase();
		return "RUNNING".equals(status) || "PROCESSING".equals(status) || job.getCompletedTime() == null;

	}

	private int countStatus(List<MigrationJob> jobs, String status) {
		return (int) jobs.stream().filter(job -> status.equalsIgnoreCase(job.getStatus())).count();
	}

	private int safeInt(Integer value) {
		return value == null ? 0 : value;
	}

	private double calculateAverageDuration(List<MigrationJob> jobs) {

		List<Long> durations = new ArrayList<>();

		for (MigrationJob job : jobs) {
			if (job.getStartedTime() == null || job.getCompletedTime() == null) {
				continue;

			}

			long seconds = Duration.between(job.getStartedTime(), job.getCompletedTime()).getSeconds();

			if (seconds >= 0) {
				durations.add(seconds);
			}

		}

		if (durations.isEmpty()) {
			return 0;

		}
		return durations.stream().mapToLong(Long::longValue).average().orElse(0);

	}

	private TrendResult buildTrend(List<MigrationJob> jobs) {

		/*
		 * Group by date.
		 */
		Map<LocalDate, int[]> grouped = new LinkedHashMap<>();

		List<LocalDate> dates = jobs.stream().filter(job -> job.getStartedTime() != null)
				.map(job -> job.getStartedTime().toLocalDate()).distinct().sorted().collect(Collectors.toList());

		for (LocalDate date : dates) {
			grouped.put(date, new int[] { 0, 0, 0 });

		}

		for (MigrationJob job : jobs) {
			if (job.getStartedTime() == null) {
				continue;

			}

			LocalDate date = job.getStartedTime().toLocalDate();
			int[] values = grouped.get(date);

			if (values == null) {
				continue;

			}

			if ("COMPLETED".equalsIgnoreCase(job.getStatus())) {
				values[0]++;

			} else if (isRunning(job)) {
				values[2]++;

			} else {
				values[1]++;

			}

		}

		TrendResult result = new TrendResult();

		for (Map.Entry<LocalDate, int[]> entry : grouped.entrySet()) {

			result.labels.add(entry.getKey().toString());
			result.successful.add(entry.getValue()[0]);
			result.failed.add(entry.getValue()[1]);
			result.running.add(entry.getValue()[2]);

		}

		return result;
	}

	private static class TrendResult {

		private final List<String> labels = new ArrayList<>();
		private final List<Integer> successful = new ArrayList<>();
		private final List<Integer> failed = new ArrayList<>();
		private final List<Integer> running = new ArrayList<>();

	}

	public byte[] exportExcel(String fromDate, String toDate, String module, String tenant) throws IOException {

		/*
		 * ===================================================== 1. LOAD JOBS
		 * =====================================================
		 */

		List<MigrationJob> jobs = migrationJobRepository.findAll();

		/*
		 * ===================================================== 2. FILTER
		 * =====================================================
		 */

		jobs = jobs.stream()

				.filter(job -> matchesDate(job, fromDate, toDate))

				.filter(job -> matchesModule(job, module))

				.filter(job -> matchesTenant(job, tenant))

				.sorted(Comparator.comparing(MigrationJob::getStartedTime,
						Comparator.nullsLast(Comparator.reverseOrder())))

				.toList();

		/*
		 * ===================================================== 3. CREATE WORKBOOK
		 * =====================================================
		 */

		try (Workbook workbook = new XSSFWorkbook();

				ByteArrayOutputStream output = new ByteArrayOutputStream()) {

			Sheet sheet = workbook.createSheet("Migration Report");

			/*
			 * ================================================= STYLES
			 * =================================================
			 */

			CellStyle titleStyle = createTitleStyle(workbook);

			CellStyle headerStyle = createHeaderStyle(workbook);

			CellStyle normalStyle = createNormalStyle(workbook);

			CellStyle successStyle = createStatusStyle(workbook, IndexedColors.GREEN);

			CellStyle errorStyle = createStatusStyle(workbook, IndexedColors.RED);

			CellStyle runningStyle = createStatusStyle(workbook, IndexedColors.ORANGE);

			/*
			 * ================================================= TITLE
			 * =================================================
			 */

			Row titleRow = sheet.createRow(0);

			titleRow.setHeightInPoints(25);

			Cell titleCell = titleRow.createCell(0);

			titleCell.setCellValue("Finance Data Migration Report");

			titleCell.setCellStyle(titleStyle);

			/*
			 * Merge title
			 */

			sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 12));

			/*
			 * ================================================= REPORT FILTER INFO
			 * =================================================
			 */

			Row filterRow = sheet.createRow(1);

			filterRow.createCell(0).setCellValue("From Date");

			filterRow.createCell(1).setCellValue(fromDate == null ? "All" : fromDate);

			filterRow.createCell(2).setCellValue("To Date");

			filterRow.createCell(3).setCellValue(toDate == null ? "All" : toDate);

			filterRow.createCell(4).setCellValue("Module");

			filterRow.createCell(5).setCellValue(
					module == null || module.isBlank() || "ALL".equalsIgnoreCase(module) ? "All Modules" : module);

			filterRow.createCell(6).setCellValue("Tenant");

			filterRow.createCell(7).setCellValue(
					tenant == null || tenant.isBlank() || "ALL".equalsIgnoreCase(tenant) ? "All Tenants" : tenant);

			/*
			 * ================================================= SUMMARY
			 * =================================================
			 */

			int totalJobs = jobs.size();

			int successfulJobs = (int) jobs.stream().filter(job -> "COMPLETED".equalsIgnoreCase(job.getStatus()))
					.count();

			int failedJobs = (int) jobs.stream().filter(job -> "FAILED".equalsIgnoreCase(job.getStatus())
					|| "COMPLETED_WITH_ERRORS".equalsIgnoreCase(job.getStatus())).count();

			int runningJobs = (int) jobs.stream().filter(this::isRunning).count();

			int totalRecords = jobs.stream().mapToInt(job -> safeInt(job.getTotalRecords())).sum();

			int successRecords = jobs.stream().mapToInt(job -> safeInt(job.getSuccessRecords())).sum();

			int failedRecords = jobs.stream().mapToInt(job -> safeInt(job.getFailedRecords())).sum();

			int skippedRecords = jobs.stream().mapToInt(job -> safeInt(job.getSkippedRecords())).sum();

			int summaryRowIndex = 3;

			Row summaryHeader = sheet.createRow(summaryRowIndex++);

			summaryHeader.createCell(0).setCellValue("Summary");

			summaryHeader.createCell(1).setCellValue("Value");

			for (Cell cell : summaryHeader) {

				cell.setCellStyle(headerStyle);

			}

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Total Jobs", totalJobs, normalStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Successful Jobs", successfulJobs, successStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Failed / Errors", failedJobs, errorStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Running Jobs", runningJobs, runningStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Total Records", totalRecords, normalStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Successful Records", successRecords, successStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Failed Records", failedRecords, errorStyle);

			summaryRowIndex = addSummaryRow(sheet, summaryRowIndex, "Skipped Records", skippedRecords, runningStyle);

			/*
			 * ================================================= JOB DETAILS
			 * =================================================
			 */

			int dataStart = summaryRowIndex + 2;

			Row header = sheet.createRow(dataStart);

			String[] headers = {

					"Job ID", "Module", "Tenant", "Status", "Total Records", "Successful", "Failed", "Skipped",
					"Progress %", "Started Time", "Completed Time", "Duration", "Message"

			};

			for (int i = 0; i < headers.length; i++) {

				Cell cell = header.createCell(i);

				cell.setCellValue(headers[i]);

				cell.setCellStyle(headerStyle);

			}

			/*
			 * ================================================= DATA
			 * =================================================
			 */

			int rowNum = dataStart + 1;

			for (MigrationJob job : jobs) {

				Row row = sheet.createRow(rowNum++);

				createStringCell(row, 0, job.getJobId(), normalStyle);

				createStringCell(row, 1, formatModule(job.getModuleCode()), normalStyle);

				createStringCell(row, 2, formatTenant(job.getTenantId()), normalStyle);

				Cell statusCell = row.createCell(3);

				statusCell.setCellValue(formatStatus(job.getStatus()));

				statusCell.setCellStyle(
						getStatusStyle(job.getStatus(), successStyle, errorStyle, runningStyle, normalStyle));

				createIntegerCell(row, 4, job.getTotalRecords());

				createIntegerCell(row, 5, job.getSuccessRecords());

				createIntegerCell(row, 6, job.getFailedRecords());

				createIntegerCell(row, 7, job.getSkippedRecords());

				createIntegerCell(row, 8, job.getProgressPercent());

				createStringCell(row, 9, formatDateTime(job.getStartedTime()), normalStyle);

				createStringCell(row, 10, formatDateTime(job.getCompletedTime()), normalStyle);

				createStringCell(row, 11, calculateDuration(job), normalStyle);

				createStringCell(row, 12, job.getCurrentMessage(), normalStyle);

			}

			/*
			 * ================================================= EXCEL FEATURES
			 * =================================================
			 */

			sheet.createFreezePane(0, dataStart + 1);

			sheet.setAutoFilter(
					new CellRangeAddress(dataStart, Math.max(dataStart, rowNum - 1), 0, headers.length - 1));

			/*
			 * Column widths
			 */

			int[] widths = {

					26, 22, 18, 24, 14, 14, 12, 12, 12, 22, 22, 16, 55

			};

			for (int i = 0; i < widths.length; i++) {

				sheet.setColumnWidth(i, widths[i] * 256);

			}

			workbook.write(output);

			return output.toByteArray();

		}
	}

	private LocalDate parseDate(String value) {

		if (value == null || value.isBlank()) {

			return null;

		}

		return LocalDate.parse(value);
	}

	private boolean matchesDate(MigrationJob job, String fromDate, String toDate) {

		if (job.getStartedTime() == null) {

			return false;

		}

		LocalDate jobDate = job.getStartedTime().toLocalDate();

		LocalDate from = parseDate(fromDate);

		LocalDate to = parseDate(toDate);

		if (from != null && jobDate.isBefore(from)) {

			return false;

		}

		if (to != null && jobDate.isAfter(to)) {

			return false;

		}

		return true;
	}

	private CellStyle createTitleStyle(Workbook workbook) {

		CellStyle style = workbook.createCellStyle();

		Font font = workbook.createFont();

		font.setBold(true);

		font.setFontHeightInPoints((short) 16);

		font.setColor(IndexedColors.WHITE.getIndex());

		style.setFont(font);

		style.setFillForegroundColor(IndexedColors.BLUE.getIndex());

		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		style.setAlignment(HorizontalAlignment.LEFT);

		style.setVerticalAlignment(VerticalAlignment.CENTER);

		return style;
	}

	private CellStyle createHeaderStyle(Workbook workbook) {

		CellStyle style = workbook.createCellStyle();

		Font font = workbook.createFont();

		font.setBold(true);

		font.setColor(IndexedColors.WHITE.getIndex());

		style.setFont(font);

		style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());

		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		style.setAlignment(HorizontalAlignment.CENTER);

		style.setVerticalAlignment(VerticalAlignment.CENTER);

		style.setBorderBottom(BorderStyle.THIN);

		return style;
	}

	private CellStyle createNormalStyle(Workbook workbook) {

		CellStyle style = workbook.createCellStyle();

		style.setVerticalAlignment(VerticalAlignment.CENTER);

		return style;
	}

	private CellStyle createStatusStyle(Workbook workbook, IndexedColors color) {

		CellStyle style = workbook.createCellStyle();

		Font font = workbook.createFont();

		font.setBold(true);

		font.setColor(color.getIndex());

		style.setFont(font);

		return style;
	}

	private int addSummaryRow(Sheet sheet, int rowIndex, String label, int value, CellStyle style) {

		Row row = sheet.createRow(rowIndex);

		Cell labelCell = row.createCell(0);

		labelCell.setCellValue(label);

		Cell valueCell = row.createCell(1);

		valueCell.setCellValue(value);

		valueCell.setCellStyle(style);

		return rowIndex + 1;
	}

	private void createStringCell(Row row, int column, String value, CellStyle style) {

		Cell cell = row.createCell(column);

		cell.setCellValue(value == null ? "" : value);

		cell.setCellStyle(style);
	}

	private void createIntegerCell(Row row, int column, Integer value) {

		Cell cell = row.createCell(column);

		cell.setCellValue(value == null ? 0 : value);
	}

	private CellStyle getStatusStyle(String status, CellStyle successStyle, CellStyle errorStyle,
			CellStyle runningStyle, CellStyle normalStyle) {

		if ("COMPLETED".equalsIgnoreCase(status)) {

			return successStyle;

		}

		if ("FAILED".equalsIgnoreCase(status) || "COMPLETED_WITH_ERRORS".equalsIgnoreCase(status)) {

			return errorStyle;

		}

		if ("RUNNING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {

			return runningStyle;

		}

		return normalStyle;
	}

	private String formatModule(String module) {

		if (module == null || module.isBlank()) {

			return "-";

		}

		return Arrays.stream(module.toLowerCase().split("_"))
				.map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
				.collect(Collectors.joining(" "));
	}

	private String formatTenant(String tenant) {

		if (tenant == null || tenant.isBlank()) {

			return "-";

		}

		String[] parts = tenant.split("\\.", 2);

		String name = parts.length > 1 ? parts[1] : parts[0];

		return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
	}

	private String formatStatus(String status) {

		if (status == null || status.isBlank()) {

			return "-";

		}

		switch (status.toUpperCase()) {

		case "COMPLETED":
			return "Success";

		case "COMPLETED_WITH_ERRORS":
			return "Completed with Errors";

		case "FAILED":
			return "Failed";

		case "RUNNING":
			return "Running";

		case "PROCESSING":
			return "Processing";

		default:
			return status;

		}
	}

	private String formatDateTime(LocalDateTime dateTime) {

		if (dateTime == null) {

			return "";

		}

		return dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
	}

	private String calculateDuration(MigrationJob job) {

		if (job.getStartedTime() == null) {

			return "";

		}

		LocalDateTime end = job.getCompletedTime() != null ? job.getCompletedTime() : LocalDateTime.now();

		long seconds = Duration.between(job.getStartedTime(), end).getSeconds();

		if (seconds < 60) {

			return seconds + "s";

		}

		long minutes = seconds / 60;

		long remaining = seconds % 60;

		if (minutes < 60) {

			return minutes + "m " + remaining + "s";

		}

		long hours = minutes / 60;

		minutes = minutes % 60;

		return hours + "h " + minutes + "m " + remaining + "s";
	}



}