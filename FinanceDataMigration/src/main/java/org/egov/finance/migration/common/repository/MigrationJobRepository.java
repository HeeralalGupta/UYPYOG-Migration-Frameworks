package org.egov.finance.migration.common.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.egov.finance.migration.common.entity.MigrationJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationJobRepository extends JpaRepository<MigrationJob, Long> {

	Optional<MigrationJob> findByJobId(String jobId);

	long countByStatus(String status);

	long countByStatusIn(List<String> statuses);

	long countByStartedTimeBetween(LocalDateTime start, LocalDateTime end);

	List<MigrationJob> findTop10ByOrderByStartedTimeDesc();
	
	List<MigrationJob> findByStartedTimeBetween(
	        LocalDateTime start,
	        LocalDateTime end
	);
	
}