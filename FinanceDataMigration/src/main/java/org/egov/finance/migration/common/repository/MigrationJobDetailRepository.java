package org.egov.finance.migration.common.repository;

import java.util.List;
import java.util.Optional;

import org.egov.finance.migration.common.entity.MigrationJobDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MigrationJobDetailRepository extends JpaRepository<MigrationJobDetail, Long> {
	Optional<MigrationJobDetail> findFirstByTenantIdAndModuleCodeAndStartRowAndEndRowAndStatus(String tenantId,
			String moduleCode, Integer startRow, Integer endRow, String status);
	List<MigrationJobDetail> findByJobJobIdOrderByRecordNumberAsc(String jobId);
	
    @Query(value = """
            SELECT *
            FROM migration_job_detail
            WHERE tenant_id = :tenantId
              AND module_code = :moduleCode
              AND record_key && CAST(:recordKeys AS text[])
              AND status = :status
            LIMIT 1
            """, nativeQuery = true)
        Optional<MigrationJobDetail> findDuplicate(
                @Param("tenantId") String tenantId,
                @Param("moduleCode") String moduleCode,
                @Param("recordKeys") String[] recordKeys,
                @Param("status") String status);
}
