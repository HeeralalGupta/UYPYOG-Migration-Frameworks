package org.egov.finance.migration.modules.contractorbill.dto;

import lombok.Data;

@Data
public class EgBillPurchaseItemsDTO {
    private Long itemId;
    private String itemCode;
    private Long unitRate;
    private Long billedQuantity;
    private Long unitValueWithGst;
    private Long quantity;
    private Long amount;
}
