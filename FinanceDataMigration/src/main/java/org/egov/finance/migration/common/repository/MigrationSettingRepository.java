package org.egov.finance.migration.common.repository;

import org.egov.finance.migration.common.entity.MigrationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MigrationSettingRepository
        extends JpaRepository<MigrationSetting, String> {
}