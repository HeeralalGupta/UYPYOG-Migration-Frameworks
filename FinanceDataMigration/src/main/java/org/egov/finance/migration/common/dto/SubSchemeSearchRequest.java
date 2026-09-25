package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.modules.contractorbill.dto.IdReference;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubSchemeSearchRequest {

	private String code;

	private String name;

	private IdReference scheme;

}