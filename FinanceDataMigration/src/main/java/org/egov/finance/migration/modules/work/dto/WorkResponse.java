package org.egov.finance.migration.modules.work.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkResponse {

	private Long id;

	private String workCode;

	private Long estimateId;

	private String workName;

	private String workType;

	private Long fundId;

	private Date startDate;

	private Date endDate;

	private boolean active;

	private BigDecimal estimateValue;

	private String estimateUnit;

	private String displayName;
}
