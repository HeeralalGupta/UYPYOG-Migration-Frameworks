package org.egov.finance.migration.modules.expensebill.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EgBillChecklist {

	private Long id;
	private String checklistvalue;
	private AppConfigValue appconfigvalue;
}