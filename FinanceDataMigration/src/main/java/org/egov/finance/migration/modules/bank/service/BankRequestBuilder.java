package org.egov.finance.migration.modules.bank.service;

import org.egov.finance.migration.common.dto.Bank;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.bank.dto.BankRecord;
import org.egov.finance.migration.modules.bank.dto.CreateBankRequest;
import org.springframework.stereotype.Service;

@Service
public class BankRequestBuilder {

    private final RequestInfoBuilder requestInfoBuilder;

    public BankRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
        this.requestInfoBuilder = requestInfoBuilder;
    }

    /**
     * Build Finance API BankRequest from one BankRecord.
     */
    public CreateBankRequest build(BankRecord record, MigrationRequest migrationRequest) {

        CreateBankRequest request = new CreateBankRequest();

        /*
         * Tenant
         */
        String tenantId = migrationRequest.getTenantId();
        request.setTenantId(tenantId);

        /*
         * RequestInfo
         */
        RequestInfo requestInfo = requestInfoBuilder.build(tenantId);
        request.setRequestInfo(requestInfo);

        /*
         * Build one Finance Bank
         */
        Bank bank = buildBank(record);

        /*
         * Add bank to request
         */
        request.setBank(bank);

        return request;
    }

    /**
     * Convert migration BankRecord into Finance API Bank DTO.
     */
    private Bank buildBank(BankRecord record) {

        Bank bank = new Bank();

        /*
         * Bank information
         */
        bank.setCode(null);
        bank.setName(record.getBankName());
        bank.setNarration(record.getNarration());

        /*
         * Default values
         */
        bank.setIsactive(true);

        return bank;
    }
}