package org.egov.finance.migration.modules.bankbranch.service;

import org.egov.finance.migration.common.dto.BankBranch;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.bankbranch.dto.BankBranchRecord;
import org.egov.finance.migration.modules.bankbranch.dto.CreateBankBranchRequest;
import org.springframework.stereotype.Service;

@Service
public class BankBranchRequestBuilder {

    private final RequestInfoBuilder requestInfoBuilder;

    public BankBranchRequestBuilder(
            RequestInfoBuilder requestInfoBuilder) {

        this.requestInfoBuilder = requestInfoBuilder;
    }

    /**
     * Build Finance API BankBranchRequest
     * from one BankBranchRecord.
     */
    public CreateBankBranchRequest build(
            BankBranchRecord record,
            MigrationRequest migrationRequest) {

        CreateBankBranchRequest request =
                new CreateBankBranchRequest();

        /*
         * Tenant
         */
        String tenantId =
                migrationRequest.getTenantId();

        request.setTenantId(tenantId);

        /*
         * RequestInfo
         */
        RequestInfo requestInfo =
                requestInfoBuilder.build(tenantId);

        request.setRequestInfo(requestInfo);

        /*
         * Build one Finance Bank Branch
         */
        BankBranch bankBranch =
                buildBankBranch(record);

        /*
         * Add bank branch to request
         */
        request.setBankbranch(bankBranch);

        return request;
    }

    /**
     * Convert migration BankBranchRecord
     * into Finance API BankBranch DTO.
     */
    private BankBranch buildBankBranch(
            BankBranchRecord record) {

        BankBranch bankBranch =
                new BankBranch();

        /*
         * Bank Branch information
         */
        bankBranch.setId(null);
        bankBranch.setBranchcode(
                record.getBranchCode());

        bankBranch.setBranchname(
                record.getBranchName());

        bankBranch.setIfscCode(
                record.getIfscCode());

        bankBranch.setBranchaddress1(
                record.getAddress());

        bankBranch.setContactperson(
                record.getContactPerson());

        bankBranch.setBranchphone(
                record.getPhoneNumber());

        bankBranch.setBranchMICR(
                record.getMicr());

        bankBranch.setNarration(
                record.getNarration());

        /*
         * Default values
         */
        bankBranch.setIsactive(true);

        return bankBranch;
    }
}