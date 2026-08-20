package org.egov.finance.migration.common.repository;

import java.util.Optional;

import org.egov.finance.migration.common.entity.MigrationJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationJobRepository extends JpaRepository<MigrationJob, Long> {
	Optional<MigrationJob> findByJobId(String jobId);
}
