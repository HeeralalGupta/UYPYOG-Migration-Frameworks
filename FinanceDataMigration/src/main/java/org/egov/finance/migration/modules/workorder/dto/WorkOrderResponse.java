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
public class WorkOrderResponse {

    private Long id;

    private String orderNumber;

    private String name;

    private Date orderDate;

    private Long contractorId;

    private BigDecimal orderValue;

    private BigDecimal advancePayable;

    private String description;

    private Long fundId;

    private String department;

    private Long schemeId;

    private Long subSchemeId;

    private String sanctionNumber;

    private Date sanctionDate;

    private BigDecimal emdAmount;

    private BigDecimal bgAmount;

    private BigDecimal estimatedBudget;

    private BigDecimal apbg;

    private Boolean active;

    private String departmentName;

    private Boolean editAllFields;

    private String orderType;

    private Long workId;

    private String workCode;

    private List<WorkOrderItemsResponse> workItems;
}