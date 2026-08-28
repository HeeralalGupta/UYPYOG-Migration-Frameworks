package org.egov.finance.migration.modules.workorder.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderItemsResponse {

    private Long id;

    private String itemCode;

    private String unit;

    private BigDecimal unitRate;

    private BigDecimal quantity;

    private Long gstRate;

    private BigDecimal unitValueWithGst;

    private BigDecimal amount;

    private BigDecimal glcodeid;

    private String orderNumber;
}
