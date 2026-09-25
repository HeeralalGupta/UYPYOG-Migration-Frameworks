package org.egov.finance.migration.processor;

import java.util.List;

import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.MigrationResult;

public abstract class AbstractMigrationProcessor implements MigrationProcessor {

	@Override
	public MigrationResult process(MigrationRequest request) {

		beforeProcess(request);
		MigrationResult result = doProcess(request);
		afterProcess(request, result);
		return result;
	}

	protected void beforeProcess(MigrationRequest request) {
		// Common pre-processing
	}

	protected abstract MigrationResult doProcess(MigrationRequest request);

	protected void afterProcess(MigrationRequest request, MigrationResult result) {

		// Common post-processing

	}
	
    /**
     * Unique value for duplicate detection.
     */
	protected List<String> getRecordKeys(Object record) {
		return null;
	}
	
    /**
     * Normalize value for duplicate detection.
     */
    protected String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim()
                .toUpperCase()
                .replaceAll("\\s+", " ");
    }

}