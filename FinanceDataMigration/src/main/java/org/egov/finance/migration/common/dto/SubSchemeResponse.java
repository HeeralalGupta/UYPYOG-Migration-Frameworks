package org.egov.finance.migration.common.dto;

import java.util.List;

import org.egov.finance.migration.common.util.Pagination;
import org.egov.finance.migration.modules.journalvoucher.response.ResponseInfo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubSchemeResponse {

	private ResponseInfo responseInfo;

	private List<SubScheme> subSchemes;

	private Pagination pagination;

}