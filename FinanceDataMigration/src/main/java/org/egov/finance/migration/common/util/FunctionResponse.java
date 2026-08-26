package org.egov.finance.migration.common.util;

import java.util.List;

import org.egov.finance.migration.common.dto.Function;
import org.egov.finance.migration.modules.journalvoucher.response.ResponseInfo;

import lombok.Data;

@Data
public class FunctionResponse {
    private ResponseInfo responseInfo;
    private List<Function> functions;
    private Pagination pagination;
}
