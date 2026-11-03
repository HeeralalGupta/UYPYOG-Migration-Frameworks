package org.egov.finance.migration.modules.banktobank.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankToBankTransferRequest {

	private String voucherDate;;

    // Transfer From
    private String fromFund;

    private String  fromSchemeName;

    private String fromDepartmentName;

    private String functionName;
    
    private String fromBankName;

    private String fromBankAccountNumber;

    // Transfer To
    private String toFund;
    
    private String toDepartmentName;
    
    // Transfer From
    private String fromInterFundCode;
    
    private String toInterFundCode;
    
    private String toBankName;

    private String toBankAccountNumber;

    private String modeOfCollection;

    private String referenceNumber;

    private String referenceDate;

    private BigDecimal amount;
    
    private String description;

}