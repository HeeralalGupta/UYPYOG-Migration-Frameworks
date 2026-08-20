package org.egov.finance.migration.modules.journalvoucher.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class Voucher {
	@JsonIgnore
	private int startRow;
	@JsonIgnore
	private int endRow;
    private String name;
    private String type;
    private String voucherNumber;
    private String description;
    private String voucherDate;
    private String department;
    private String source;
    private String serviceName;
    private Long moduleId;
    private FundContract fund;
    private FunctionContract function;
    private SchemeContract scheme;
    private SubSchemeContract subScheme;
    private List<AccountDetailContract> ledgers = new ArrayList<>();

}
