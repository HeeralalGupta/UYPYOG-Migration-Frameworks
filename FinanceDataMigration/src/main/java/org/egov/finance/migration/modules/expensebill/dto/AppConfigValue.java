package org.egov.finance.migration.modules.expensebill.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppConfigValue {

	private Long id;
	private String value;
	private Date effectiveFrom;
	private AppConfig config;
}
