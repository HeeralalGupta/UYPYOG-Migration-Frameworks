package org.egov.finance.migration.common.util;

import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class Pagination {
	public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_PAGE_OFFSET = 0;

    private Integer totalResults;

    private Integer totalPages;

    @Max(500l)
    private Integer pageSize = Integer.valueOf(DEFAULT_PAGE_SIZE);

    private Integer currentPage;

    private Integer offSet = Integer.valueOf(DEFAULT_PAGE_OFFSET);
}
