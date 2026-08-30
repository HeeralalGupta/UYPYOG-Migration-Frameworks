package org.egov.finance.migration.modules.expensebill.response;

import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.PageContract;
import org.egov.finance.migration.modules.expensebill.dto.ExpenseBillRequest;
import org.egov.finance.migration.modules.journalvoucher.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseBillResponse {
	@JsonProperty("ExpenseBill")
	private List<ExpenseBillRequest> expenseBillRequest = new ArrayList<>(0);

	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	private PageContract page;
}
