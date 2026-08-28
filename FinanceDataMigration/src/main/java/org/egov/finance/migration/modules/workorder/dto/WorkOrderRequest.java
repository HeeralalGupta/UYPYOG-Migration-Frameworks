package org.egov.finance.migration.modules.workorder.dto;

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
public class WorkOrderRequest {

    private String name;

    private Date orderDate;

    private String contractorName;

    private BigDecimal advancePayable;

    private String description;

    private String fundName;

    private String departmentName;

    private String schemeName;

    private String subSchemeName;

    private String sanctionNumber;

    private Date sanctionDate;

    private BigDecimal emdAmount;

    private BigDecimal bgAmount;

    private BigDecimal estimatedBudget;

    private BigDecimal apbg;

    @Builder.Default
    private Boolean active = true;

    private String orderType;

    private String workName;
    
    private String workCode;
    
    private BigDecimal orderValue; 

    private List<WorkOrderItemsRequest> workItems;

}