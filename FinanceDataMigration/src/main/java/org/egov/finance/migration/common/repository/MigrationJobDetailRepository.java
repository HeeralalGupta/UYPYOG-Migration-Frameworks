package org.egov.finance.migration.common.repository;

import java.util.List;
import java.util.Optional;

import org.egov.finance.migration.common.entity.MigrationJobDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationJobDetailRepository extends JpaRepository<MigrationJobDetail, Long> {
	Optional<MigrationJobDetail> findFirstByTenantIdAndModuleCodeAndStartRowAndEndRowAndStatus(String tenantId,
			String moduleCode, Integer startRow, Integer endRow, String status);
	List<MigrationJobDetail> findByJobJobIdOrderByRecordNumberAsc(String jobId);
}
