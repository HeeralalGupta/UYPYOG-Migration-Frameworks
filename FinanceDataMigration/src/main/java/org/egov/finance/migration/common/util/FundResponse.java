package org.egov.finance.migration.common.util;

import java.util.List;

import org.egov.finance.migration.common.dto.Fund;
import org.egov.finance.migration.modules.journalvoucher.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FundResponse {
	
	private ResponseInfo responseInfo;
	private List<Fund> funds;
	@JsonProperty("page")
	private Pagination page;
	
}
