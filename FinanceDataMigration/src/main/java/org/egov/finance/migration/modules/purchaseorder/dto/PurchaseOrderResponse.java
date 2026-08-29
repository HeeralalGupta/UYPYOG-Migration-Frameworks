package org.egov.finance.migration.modules.purchaseorder.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.egov.finance.migration.modules.subscheme.dto.SubSchemeResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderResponse {

    private Long id;

    private String orderNumber;

    private String name;

    private Date orderDate;

    private Long supplierId;
    
    private String supplierName;

    private BigDecimal orderValue;

    private BigDecimal advancePayable;

    private String description;

    private Long fundId;
    
    private String fundName;

    private String department;

    private Long schemeId;
    
    private String schemeName;

    private SubSchemeResponse subScheme;

    private String sanctionNumber;

    private Date sanctionDate;

    private Boolean active;

    private String departmentName;

    private Boolean editAllFields;

    private List<PurchaseItemsResponse> purchaseItems;
}