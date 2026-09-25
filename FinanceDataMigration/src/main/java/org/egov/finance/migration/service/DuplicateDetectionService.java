package org.egov.finance.migration.service;

import java.util.List;

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
            List<String> recordKeys) {

        if (recordKeys == null || recordKeys.isEmpty()) {
            return false;
        }

        String[] keys = recordKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toArray(String[]::new);

        if (keys.length == 0) {
            return false;
        }

        return repository.findDuplicate(
                tenantId,
                moduleCode,
                keys,
                "SUCCESS"
        ).isPresent();
    }
}

