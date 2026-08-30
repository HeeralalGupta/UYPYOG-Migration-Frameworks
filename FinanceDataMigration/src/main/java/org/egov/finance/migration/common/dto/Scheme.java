package org.egov.finance.migration.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Scheme {

	private Long id;
	private String code;
	private String name;
	private Long validFrom;
	private Long validTo;
	private Boolean isActive;
	private String description;
	private Fund fund;

}