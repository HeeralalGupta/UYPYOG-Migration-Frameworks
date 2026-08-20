package org.egov.finance.migration.modules.journalvoucher.dto;

import java.util.ArrayList;
import java.util.List;

import org.egov.finance.migration.common.dto.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class VoucherRequest {
	
    private String tenantId;
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
    private List<Voucher> vouchers = new ArrayList<>();
    
}
