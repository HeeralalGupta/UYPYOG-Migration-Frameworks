package org.egov.finance.migration.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.finance.migration.common.dto.SettingsConfigRequest;
import org.egov.finance.migration.common.dto.SettingsResponse;
import org.egov.finance.migration.common.entity.MigrationSetting;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.common.repository.MigrationSettingRepository;
import org.egov.finance.migration.config.TenantConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



import jakarta.transaction.Transactional;

@Service
public class SettingsService {

	@Value("${migration.settings.default-page-size:10}")
	private Integer defaultPageSize;

	@Value("${migration.settings.auto-refresh-seconds:10}")
	private Integer autoRefreshSeconds;

	@Value("${migration.settings.max-upload-size-mb:20}")
	private Integer maxUploadSizeMb;

	@Value("${migration.settings.allowed-file-extensions:xlsx,xls}")
	private String allowedFileExtensions;

	@Value("${migration.settings.duplicate-policy:SKIP}")
	private String duplicatePolicy;

	@Value("${migration.settings.concurrent-migration-limit:3}")
	private Integer concurrentMigrationLimit;

	private final MigrationJobRepository migrationJobRepository;
	private final MigrationSettingRepository migrationSettingRepository;

	private final TenantConfig tenantConfig;

	@Value("${spring.application.name:Finance Data Migration}")
	private String applicationName;

	public SettingsService(MigrationJobRepository migrationJobRepository, TenantConfig tenantConfig,
			MigrationSettingRepository migrationSettingRepository) {

		this.migrationJobRepository = migrationJobRepository;
		this.tenantConfig = tenantConfig;
		this.migrationSettingRepository = migrationSettingRepository;
	}

	public SettingsResponse getSettings() {

		SettingsResponse response = new SettingsResponse();

		String pageSize = getSetting("defaultPageSize", String.valueOf(defaultPageSize));
		String refresh = getSetting("autoRefreshSeconds", String.valueOf(autoRefreshSeconds));
		String uploadSize = getSetting("maxUploadSizeMb", String.valueOf(maxUploadSizeMb));
		String extensions = getSetting("allowedFileExtensions", allowedFileExtensions);
		String duplicate = getSetting("duplicatePolicy", duplicatePolicy);
		String concurrent = getSetting("concurrentMigrationLimit", String.valueOf(concurrentMigrationLimit));

		response.setDefaultPageSize(Integer.parseInt(pageSize));
		response.setAutoRefreshSeconds(Integer.parseInt(refresh));
		response.setMaxUploadSizeMb(Integer.parseInt(uploadSize));
		response.setAllowedFileExtensions(extensions);
		response.setDuplicatePolicy(duplicate);
		response.setConcurrentMigrationLimit(Integer.parseInt(concurrent));

		/*
		 * ===================================================== APPLICATION
		 * =====================================================
		 */

		response.setApplicationName(applicationName);
		response.setApplicationStatus("RUNNING");
		response.setMigrationEngineStatus("AVAILABLE");

		/*
		 * ===================================================== MODULES
		 * =====================================================
		 */

		List<String> modules = Arrays.stream(MigrationType.values()).map(Enum::name).sorted().toList();

		response.setModules(modules);
		response.setTotalModules(modules.size());

		/*
		 * ===================================================== TENANTS
		 * =====================================================
		 */

		List<String> tenants = tenantConfig.getTenants().stream().map(String::trim).filter(value -> !value.isEmpty())
				.distinct().sorted().toList();

		response.setTenants(tenants);
		response.setTenantCount(tenants.size());

		/*
		 * ===================================================== JOBS
		 * =====================================================
		 */

		long runningJobs = migrationJobRepository.countByStatusIn(List.of("RUNNING", "PROCESSING"));
		response.setActiveJobs((int) runningJobs);

		/*
		 * ===================================================== DEFAULT SETTINGS
		 * =====================================================
		 */

		response.setDefaultPageSize(10);
		response.setAutoRefreshSeconds(10);

		/*
		 * Database status
		 *
		 * If this service successfully reached the repository, the database is
		 * considered available.
		 */

		response.setDatabaseStatus("CONNECTED");

		return response;
	}

	private String getSetting(String key, String defaultValue) {
		return migrationSettingRepository.findById(key).map(MigrationSetting::getSettingValue).orElse(defaultValue);
	}

	@Transactional
	public void updateSettings(SettingsConfigRequest request) {

		validateSettings(request);

		saveSetting("defaultPageSize", String.valueOf(request.getDefaultPageSize()), "Default page size");

		saveSetting("autoRefreshSeconds", String.valueOf(request.getAutoRefreshSeconds()),
				"Dashboard/history auto refresh interval");

		saveSetting("maxUploadSizeMb", String.valueOf(request.getMaxUploadSizeMb()),
				"Maximum migration upload size in MB");

		saveSetting("allowedFileExtensions", normalizeExtensions(request.getAllowedFileExtensions()),
				"Allowed migration file extensions");

		saveSetting("duplicatePolicy", request.getDuplicatePolicy().toUpperCase(), "Duplicate record handling policy");

		saveSetting("concurrentMigrationLimit", String.valueOf(request.getConcurrentMigrationLimit()),
				"Maximum simultaneous migrations");
	}

	private void saveSetting(String key, String value, String description) {

		MigrationSetting setting = migrationSettingRepository.findById(key).orElseGet(MigrationSetting::new);

		setting.setSettingKey(key);
		setting.setSettingValue(value);
		setting.setDescription(description);

		migrationSettingRepository.save(setting);
	}

	private void validateSettings(SettingsConfigRequest request) {

		if (request.getDefaultPageSize() == null || request.getDefaultPageSize() < 5
				|| request.getDefaultPageSize() > 100) {

			throw new IllegalArgumentException("Page size must be between 5 and 100");
		}

		if (request.getAutoRefreshSeconds() == null || request.getAutoRefreshSeconds() < 5
				|| request.getAutoRefreshSeconds() > 300) {

			throw new IllegalArgumentException("Auto refresh must be between 5 and 300 seconds");
		}

		if (request.getMaxUploadSizeMb() == null || request.getMaxUploadSizeMb() < 1
				|| request.getMaxUploadSizeMb() > 500) {

			throw new IllegalArgumentException("Maximum upload size must be between 1 and 500 MB");
		}

		if (request.getConcurrentMigrationLimit() == null || request.getConcurrentMigrationLimit() < 1
				|| request.getConcurrentMigrationLimit() > 20) {

			throw new IllegalArgumentException("Concurrent migration limit must be between 1 and 20");
		}

		if (request.getDuplicatePolicy() == null
				|| !Set.of("SKIP", "FAIL", "ALLOW").contains(request.getDuplicatePolicy().toUpperCase())) {

			throw new IllegalArgumentException("Invalid duplicate policy");
		}

		if (request.getAllowedFileExtensions() == null || request.getAllowedFileExtensions().isBlank()) {

			throw new IllegalArgumentException("At least one file extension is required");
		}
	}

	private String normalizeExtensions(String extensions) {

		return Arrays.stream(extensions.split(",")).map(String::trim).filter(value -> !value.isEmpty()).map(value -> {

			if (value.startsWith(".")) {

				return value.substring(1).toLowerCase();

			}

			return value.toLowerCase();

		}).distinct().collect(Collectors.joining(","));
	}

}