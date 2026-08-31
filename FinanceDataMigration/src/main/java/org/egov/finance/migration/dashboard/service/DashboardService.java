package org.egov.finance.migration.dashboard.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
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

	public DashboardResponse getDashboard() {
		DashboardResponse response = new DashboardResponse();

		/*
		 * ===================================================== 1. MIGRATION MODULES
		 * =====================================================
		 */
		response.setMigrationModules(migrationProcessorFactory.getMigrationModuleCount());

		/*
		 * ===================================================== 2. TOTAL JOBS
		 * =====================================================
		 */
		response.setTotalJobs(migrationJobRepository.count());

		/*
		 * ===================================================== 3. TODAY JOBS
		 * =====================================================
		 */

		LocalDate today = LocalDate.now();
		LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIN);
		LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);
		response.setTodayJobs(migrationJobRepository.countByStartedTimeBetween(startOfDay, endOfDay));

		/*
		 * ===================================================== 4. SUCCESSFUL JOBS
		 * =====================================================
		 */

		long successfulJobs = migrationJobRepository.countByStatus("COMPLETED");
		response.setSuccessfulJobs(successfulJobs);

		/*
		 * ===================================================== 5. FAILED JOBS
		 * =====================================================
		 *
		 * Your processors use:
		 *
		 * COMPLETED_WITH_ERRORS
		 */

		long failedJobs = migrationJobRepository.countByStatus("COMPLETED_WITH_ERRORS");
		response.setFailedJobs(failedJobs);

		/*
		 * ===================================================== 6. SUCCESS RATE
		 * =====================================================
		 */

		long completedJobs = successfulJobs + failedJobs;
		double successRate = 0.0;
		if (completedJobs > 0) {
			successRate = (successfulJobs * 100.0) / completedJobs;
		}
		response.setSuccessRate(Math.round(successRate * 10.0) / 10.0);

		/*
		 * ===================================================== 7. RUNNING JOBS
		 * =====================================================
		 */

		long runningJobs = migrationJobRepository.countByStatusIn(Arrays.asList("RUNNING", "PROCESSING"));
		response.setRunningJobs(runningJobs);

		/*
		 * ===================================================== 8. RECENT JOBS
		 * =====================================================
		 */

		List<MigrationJob> jobs = migrationJobRepository.findTop10ByOrderByStartedTimeDesc();
		response.setRecentJobs(jobs.stream().map(this::convertToRecentJob).collect(Collectors.toList()));
		return response;
	}

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

	public MigrationActivityResponse getMigrationActivity(int days) {

		if (days <= 0) {
			days = 7;
		}

		/*
		 * Prevent someone from requesting an unnecessarily large range from the
		 * dashboard.
		 */
		if (days > 90) {
			days = 90;
		}

		LocalDate today = LocalDate.now();
		LocalDate startDate = today.minusDays(days - 1);
		LocalDateTime startDateTime = startDate.atStartOfDay();
		LocalDateTime endDateTime = today.plusDays(1).atStartOfDay();
		List<MigrationJob> jobs = migrationJobRepository.findByStartedTimeBetween(startDateTime, endDateTime);

		/*
		 * ===================================================== PREPARE RESPONSE
		 * =====================================================
		 */

		MigrationActivityResponse response = new MigrationActivityResponse();
		List<String> labels = new java.util.ArrayList<>();
		List<Long> successful = new java.util.ArrayList<>();
		List<Long> failed = new java.util.ArrayList<>();
		List<Long> running = new java.util.ArrayList<>();

		/*
		 * ===================================================== BUILD DAILY DATA
		 * =====================================================
		 */

		for (int i = 0; i < days; i++) {

			LocalDate date = startDate.plusDays(i);
			labels.add(date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));

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
				 * Currently running
				 */
				else if ("RUNNING".equalsIgnoreCase(status) || "PROCESSING".equalsIgnoreCase(status)) {
					runningCount++;
				}
			}

			successful.add(successCount);
			failed.add(failedCount);
			running.add(runningCount);
		}

		response.setLabels(labels);
		response.setSuccessful(successful);
		response.setFailed(failed);
		response.setRunning(running);

		return response;
	}

}