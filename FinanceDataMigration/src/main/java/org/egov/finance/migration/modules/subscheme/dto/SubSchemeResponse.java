package org.egov.finance.migration.modules.subscheme.dto;

import java.math.BigDecimal;
import java.sql.Date;

import org.egov.finance.migration.common.enums.LoanCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubSchemeResponse {
	private Long id;

	private String code;

	private String name;

	private Date validFrom;

	private Date validTo;

	private Boolean isActive;

	private Long schemeId;

	private String schemeCode;

	private String schemeName;

	private Long departmentId;

	private String departmentName;

	private LoanCategory loanCategory;

	private BigDecimal initialEstimateAmount;

	private String councilLoanProposalNumber;

	private Date councilLoanProposalDate;

	private String councilAdminSanctionNumber;

	private Date councilAdminSanctionDate;

	private String govtLoanProposalNumber;

	private Date govtLoanProposalDate;

	private String govtAdminSanctionNumber;

	private Date govtAdminSanctionDate;

	private Date createdDate;

	private Long createdBy;

	private Long lastModifiedBy;

	private Date lastModifiedDate;
}

