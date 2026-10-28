package org.egov.finance.migration.modules.billpayment.response;

import lombok.Data;

@Data
public class BillPaymentVoucherResponse {

    private Long paymentVoucherId;
    private String voucherNumber;
    private String message;
}
