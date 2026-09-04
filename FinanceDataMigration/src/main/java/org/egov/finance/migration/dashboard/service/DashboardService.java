package org.egov.finance.migration.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.dashboard.dto.DashboardResponse;
import org.egov.finance.migration.dashboard.dto.MigrationActivityResponse;
import org.egov.finance.migration.dashboard.dto.RecentMigrationJob;
import org.egov.finance.migration.factory.MigrationProcessorFactory;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

	private final MigrationJobRepository migrationJobRepository;
	private final MigrationProcessorFactory migrationProcessorFactory;

	public DashboardService(MigrationJobRepository migrationJobRepository,
			MigrationProcessorFactory migrationProcessorFactory) {

		this.migrationJobRepository = migrationJobRepository;
		this.migrationProcessorFactory = migrationProcessorFactory;
	}

	/*
	 * ============================================================ DASHBOARD
	 * ============================================================
	 */
	public DashboardResponse getDashboard(String tenantId) {

		DashboardResponse response = new DashboardResponse();

		/*
		 * ======================================================== GET JOBS
		 * ========================================================
		 */
		List<MigrationJob> allJobs = migrationJobRepository.findAll();

		/*
		 * ======================================================== FILTER BY TENANT
		 * ========================================================
		 *
		 * If tenantId is provided, show only that tenant's jobs.
		 *
		 * If tenantId is blank/null, existing behavior is retained and all jobs are
		 * used.
		 */
		List<MigrationJob> jobs;

		if (tenantId == null || tenantId.isBlank()) {

			jobs = allJobs;

		} else {

			jobs = allJobs.stream()
					.filter(job -> job.getTenantId() != null && tenantId.equalsIgnoreCase(job.getTenantId()))
					.collect(Collectors.toList());
		}

		/*
		 * ======================================================== 1. MIGRATION MODULES
		 * ========================================================
		 *
		 * This is application-wide, so it is not filtered by tenant.
		 */
		response.setMigrationModules(migrationProcessorFactory.getMigrationModuleCount());

		/*
		 * ======================================================== 2. TOTAL JOBS
		 * ========================================================
		 */
		response.setTotalJobs(Long.valueOf(jobs.size()));

		/*
		 * ======================================================== 3. TODAY JOBS
		 * ========================================================
		 */
		LocalDate today = LocalDate.now();

		long todayJobs = jobs.stream().filter(job -> job.getStartedTime() != null)
				.filter(job -> job.getStartedTime().toLocalDate().equals(today)).count();

		response.setTodayJobs(todayJobs);

		/*
		 * ======================================================== 4. SUCCESSFUL JOBS
		 * ========================================================
		 *
		 * Your current application uses COMPLETED.
		 */
		long successfulJobs = jobs.stream().filter(job -> "COMPLETED".equalsIgnoreCase(job.getStatus())).count();

		response.setSuccessfulJobs(successfulJobs);

		/*
		 * ======================================================== 5. FAILED JOBS
		 * ========================================================
		 *
		 * Your existing application uses COMPLETED_WITH_ERRORS.
		 *
		 * FAILED is also included so that jobs failed by MigrationAsyncService are
		 * counted.
		 */
		long failedJobs = jobs.stream().filter(job -> {

			String status = job.getStatus();

			return "COMPLETED_WITH_ERRORS".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
		}).count();

		response.setFailedJobs(failedJobs);

		/*
		 * ======================================================== 6. SUCCESS RATE
		 * ========================================================
		 */
		long completedJobs = successfulJobs + failedJobs;

		double successRate = 0.0;

		if (completedJobs > 0) {

			successRate = (successfulJobs * 100.0) / completedJobs;
		}

		response.setSuccessRate(Math.round(successRate * 10.0) / 10.0);

		/*
		 * ======================================================== 7. RUNNING JOBS
		 * ========================================================
		 */
		long runningJobs = jobs.stream().filter(job -> {

			String status = job.getStatus();

			return "RUNNING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status);
		}).count();

		response.setRunningJobs(runningJobs);

		/*
		 * ======================================================== 8. RECENT JOBS
		 * ========================================================
		 */
		List<MigrationJob> recentJobs = jobs.stream().filter(job -> job.getStartedTime() != null)
				.sorted(Comparator.comparing(MigrationJob::getStartedTime, Comparator.reverseOrder())).limit(10)
				.collect(Collectors.toList());

		response.setRecentJobs(recentJobs.stream().map(this::convertToRecentJob).collect(Collectors.toList()));

		return response;
	}

	/*
	 * ============================================================ CONVERT JOB TO
	 * RECENT JOB DTO ============================================================
	 */
	private RecentMigrationJob convertToRecentJob(MigrationJob job) {

		RecentMigrationJob recent = new RecentMigrationJob();

		recent.setJobId(job.getJobId());

		recent.setModule(job.getModuleCode());

		recent.setTenant(job.getTenantId());

		recent.setStatus(job.getStatus());

		recent.setTotalRecords(job.getTotalRecords());

		recent.setSuccessRecords(job.getSuccessRecords());

		recent.setFailedRecords(job.getFailedRecords());

		recent.setSkippedRecords(job.getSkippedRecords());

		recent.setStartedAt(job.getStartedTime());

		recent.setCompletedAt(job.getCompletedTime());

		recent.setProgressPercent(job.getProgressPercent());

		recent.setCurrentMessage(job.getCurrentMessage());

		return recent;
	}

	/*
	 * ============================================================ MIGRATION
	 * ACTIVITY ============================================================
	 */
	public MigrationActivityResponse getMigrationActivity(int days, String tenantId) {

		/*
		 * Default days
		 */
		if (days <= 0) {
			days = 7;
		}

		/*
		 * Prevent unnecessarily large range
		 */
		if (days > 90) {
			days = 90;
		}

		LocalDate today = LocalDate.now();

		LocalDate startDate = today.minusDays(days - 1);

		LocalDateTime startDateTime = startDate.atStartOfDay();

		LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();

		/*
		 * ======================================================== GET JOBS WITHIN DATE
		 * RANGE ========================================================
		 */
		List<MigrationJob> jobs = migrationJobRepository.findByStartedTimeBetween(startDateTime, endDateTime);

		/*
		 * ======================================================== FILTER BY TENANT
		 * ========================================================
		 */
		if (tenantId != null && !tenantId.isBlank()) {

			jobs = jobs.stream()
					.filter(job -> job.getTenantId() != null && tenantId.equalsIgnoreCase(job.getTenantId()))
					.collect(Collectors.toList());
		}

		/*
		 * ======================================================== PREPARE RESPONSE
		 * ========================================================
		 */
		MigrationActivityResponse response = new MigrationActivityResponse();

		List<String> labels = new ArrayList<>();

		List<Long> successful = new ArrayList<>();

		List<Long> failed = new ArrayList<>();

		List<Long> running = new ArrayList<>();

		/*
		 * ======================================================== BUILD DAILY DATA
		 * ========================================================
		 */
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");

		for (int i = 0; i < days; i++) {

			LocalDate date = startDate.plusDays(i);

			labels.add(date.format(formatter));

			long successCount = 0;
			long failedCount = 0;
			long runningCount = 0;

			for (MigrationJob job : jobs) {

				if (job.getStartedTime() == null) {
					continue;
				}

				LocalDate jobDate = job.getStartedTime().toLocalDate();

				if (!jobDate.equals(date)) {
					continue;
				}

				String status = job.getStatus();

				if (status == null) {
					continue;
				}

				/*
				 * Successful
				 */
				if ("COMPLETED".equalsIgnoreCase(status)) {

					successCount++;
				}

				/*
				 * Failed
				 */
				else if ("COMPLETED_WITH_ERRORS".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {

					failedCount++;
				}

				/*
				 * Running
				 */
				else if ("RUNNING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {

					runningCount++;
				}
			}

			successful.add(successCount);

			failed.add(failedCount);

			running.add(runningCount);
		}

		/*
		 * ======================================================== SET RESPONSE
		 * ========================================================
		 */
		response.setLabels(labels);
		response.setSuccessful(successful);
		response.setFailed(failed);
		response.setRunning(running);

		return response;
	}
}