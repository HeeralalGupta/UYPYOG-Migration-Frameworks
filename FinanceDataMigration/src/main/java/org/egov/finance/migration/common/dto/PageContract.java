package org.egov.finance.migration.common.dto;

import lombok.Data;

@Data
public class PageContract {

    private Long totalResults;
    private Long totalPages;
    private Long pageSize;
    private Long currentPage;
    private Long offSet;
}
