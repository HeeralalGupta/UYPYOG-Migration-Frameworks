package org.egov.finance.migration.modules.purchaseorder.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    private String name;

    private Date orderDate;

    private String supplierName;

    private BigDecimal orderValue;

    private BigDecimal advancePayable;

    private String description;

    private String fundName;

    private String departmentName;

    private String schemeName;

    private String subSchemeName;

    private String sanctionNumber;

    private Date sanctionDate;

    private List<PurchaseItemsRequest> purchaseItems;
}
