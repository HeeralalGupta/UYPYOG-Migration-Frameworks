package org.egov.finance.migration.modules.expensebill.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfig {
	private Long id;
	private String keyName;
	private Module module;
	private String description;
}
