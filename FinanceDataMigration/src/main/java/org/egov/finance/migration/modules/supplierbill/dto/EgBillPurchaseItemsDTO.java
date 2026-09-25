package org.egov.finance.migration.modules.supplierbill.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EgBillPurchaseItemsDTO {
    private Long itemId;
    private String itemCode;
    private Long unitRate;
    private Long billedQuantity;
    private Long unitValueWithGst;
    private Long quantity;
    private Long amount;
}
