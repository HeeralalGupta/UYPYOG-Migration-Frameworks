package org.egov.finance.migration.controller;

import org.egov.finance.migration.common.dto.SettingsConfigRequest;
import org.egov.finance.migration.common.dto.SettingsResponse;
import org.egov.finance.migration.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migration/settings")
public class SettingsController {

	private final SettingsService settingsService;

	public SettingsController(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	@GetMapping("/data")
	public ResponseEntity<SettingsResponse> getSettings() {
		return ResponseEntity.ok(settingsService.getSettings());
	}

	@PutMapping("/config")
	public ResponseEntity<?> updateSettings(@RequestBody SettingsConfigRequest request) {

		settingsService.updateSettings(request);

		return ResponseEntity.ok(settingsService.getSettings());
	}
}
