package org.egov.finance.migration.processor;

import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.MigrationResult;
import org.egov.finance.migration.common.enums.MigrationType;

public interface MigrationProcessor {

    MigrationType getMigrationType();

    MigrationResult process(MigrationRequest request);

}