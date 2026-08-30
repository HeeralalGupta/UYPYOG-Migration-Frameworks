package org.egov.finance.migration.modules.supplier.service;

import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.common.dto.Supplier;
import org.egov.finance.migration.modules.supplier.dto.CreateSupplierRequest;
import org.egov.finance.migration.modules.supplier.dto.SupplierRecord;
import org.springframework.stereotype.Service;

@Service
public class SupplierRequestBuilder {

    private final RequestInfoBuilder requestInfoBuilder;

    public SupplierRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
        this.requestInfoBuilder = requestInfoBuilder;
    }

    /**
     * Build Finance API SupplierRequest from one SupplierRecord.
     */
    public CreateSupplierRequest build(
            SupplierRecord record,
            MigrationRequest migrationRequest) {

        CreateSupplierRequest request = new CreateSupplierRequest();

        /*
         * =====================================================
         * TENANT
         * =====================================================
         */
        String tenantId = migrationRequest.getTenantId();

        request.setTenantId(tenantId);

        /*
         * =====================================================
         * REQUEST INFO
         * =====================================================
         */
        RequestInfo requestInfo = requestInfoBuilder.build(tenantId);

        request.setRequestInfo(requestInfo);

        /*
         * =====================================================
         * BUILD SUPPLIER
         * =====================================================
         */
        Supplier supplier = buildSupplier(record);

        /*
         * =====================================================
         * ADD SUPPLIER TO REQUEST
         * =====================================================
         */
        request.setBankName(record.getBankName());
        request.setBranchName(record.getBranchName());

        /*
         * Status ID
         */
        request.setStatusId(108);

        request.setSupplier(supplier);

        return request;
    }

    /**
     * Convert SupplierRecord into Finance API Supplier DTO.
     */
    private Supplier buildSupplier(SupplierRecord record) {

        Supplier supplier = new Supplier();

        /*
         * =====================================================
         * BASIC INFORMATION
         * =====================================================
         */

        supplier.setCode(record.getCode());

        supplier.setName(record.getName());

        supplier.setCorrespondenceAddress(
                record.getCorrespondenceAddress());

        supplier.setPaymentAddress(
                record.getPaymentAddress());

        supplier.setContactPerson(
                record.getContactPerson());

        supplier.setEmail(
                record.getEmail());

        supplier.setNarration(
                record.getNarration());

        /*
         * =====================================================
         * PAN / GST
         * =====================================================
         */

        supplier.setPanNumber(
                record.getPanNumber());

        supplier.setTinNumber(
                record.getTinNumber());

        supplier.setGstReason(
                record.getGstReason());

        supplier.setPanReason(
                record.getPanReason());

        /*
         * =====================================================
         * CONTACT
         * =====================================================
         */

        supplier.setMobileNumber(
                record.getMobileNumber());

        /*
         * =====================================================
         * BANK INFORMATION
         * =====================================================
         */

        supplier.setIfscCode(
                record.getIfscCode());

        supplier.setBankAccount(
                record.getBankAccount());

        /*
         * =====================================================
         * SUPPLIER INFORMATION
         * =====================================================
         */

        supplier.setSource(
                record.getSource());

        supplier.setRegistrationNumber(
                record.getRegistrationNumber());

        supplier.setEpfNumber(
                record.getEpfNumber());

        supplier.setEsiNumber(
                record.getEsiNumber());

        supplier.setGstRegisteredState(
                record.getGstRegisteredState());

        /*
         * =====================================================
         * SUPPLIER TYPE
         * =====================================================
         *
         * Use this only if Supplier DTO contains supplierType.
         */
        supplier.setSupplierType(
                record.getSupplierType());

        /*
         * =====================================================
         * STATUS
         * =====================================================
         *
         * Do not create EgwStatus here.
         *
         * Status is being passed separately through statusId.
         */

        return supplier;
    }
}