package org.egov.finance.migration.common.dto;

import java.util.List;

import org.egov.finance.migration.common.util.Pagination;
import org.egov.finance.migration.modules.journalvoucher.response.ResponseInfo;

public class SchemeResponse {

	private ResponseInfo responseInfo;

	private List<Scheme> schemes;

	private Pagination pagination;

	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
	}

	public List<Scheme> getSchemes() {
		return schemes;
	}

	public void setSchemes(List<Scheme> schemes) {
		this.schemes = schemes;
	}

	public Pagination getPagination() {
		return pagination;
	}

	public void setPagination(Pagination pagination) {
		this.pagination = pagination;
	}
}