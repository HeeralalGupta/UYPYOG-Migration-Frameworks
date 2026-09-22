package org.egov.finance.migration.service;

import org.egov.finance.migration.common.repository.MigrationJobDetailRepository;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetectionService {

	private final MigrationJobDetailRepository repository;

	public DuplicateDetectionService(MigrationJobDetailRepository repository) {
		this.repository = repository;
	}

	
    public boolean isAlreadyMigrated(
            String tenantId,
            String moduleCode,
            String recordKey) {
    	
        if (recordKey == null || recordKey.isBlank()) {
            return false;
        }

        return repository
                .findFirstByTenantIdAndModuleCodeAndRecordKeyAndStatus(
                        tenantId,
                        moduleCode,
                        recordKey,
                        "SUCCESS")
                .isPresent();
    }
}

