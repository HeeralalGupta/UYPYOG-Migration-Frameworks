package org.egov.finance.migration.modules.journalvoucher.response;



import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.PageContract;
import org.egov.finance.migration.modules.journalvoucher.dto.Voucher;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class VoucherResponse {

    @JsonProperty("Vouchers")
    private List<Voucher> vouchers = new ArrayList<>(0);

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    private PageContract page;
}
