package org.egov.finance.migration.modules.billpayment.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentBillRecord {

    private String billNumber;
    private BigDecimal paymentAmount;
}
