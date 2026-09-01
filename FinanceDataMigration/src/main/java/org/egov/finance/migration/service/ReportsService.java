package org.egov.finance.migration.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

		jobs = jobs.stream()
				.filter(job -> matchesDate(job, fromDate, toDate))
				.filter(job -> matchesModule(job, module))
				.filter(job -> matchesTenant(job, tenant))
				.collect(Collectors.toList());

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

}