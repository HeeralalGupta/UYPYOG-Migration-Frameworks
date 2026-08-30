package org.egov.finance.migration.modules.work.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkRequest {

	private String workName;


	private String workType;

	private String fundName;

	private Date startDate;

	private Date endDate;

	private BigDecimal estimateValue;
}
