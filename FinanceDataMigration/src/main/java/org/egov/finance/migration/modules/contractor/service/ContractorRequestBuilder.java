package org.egov.finance.migration.modules.contractor.service;

import org.egov.finance.migration.common.dto.Contractor;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.RequestInfo;
import org.egov.finance.migration.common.dto.RequestInfoBuilder;
import org.egov.finance.migration.modules.contractor.dto.ContractorRecord;
import org.egov.finance.migration.modules.contractor.dto.CreateContractorRequest;
import org.springframework.stereotype.Service;

@Service
public class ContractorRequestBuilder {

	private final RequestInfoBuilder requestInfoBuilder;

	public ContractorRequestBuilder(RequestInfoBuilder requestInfoBuilder) {
		this.requestInfoBuilder = requestInfoBuilder;
	}

	/**
	 * Build Finance API ContractorRequest from one ContractorRecord.
	 */
	public CreateContractorRequest build(ContractorRecord record, MigrationRequest migrationRequest) {

		CreateContractorRequest request = new CreateContractorRequest();

		/*
		 * ===================================================== TENANT
		 * =====================================================
		 */
		String tenantId = migrationRequest.getTenantId();

		request.setTenantId(tenantId);

		/*
		 * ===================================================== REQUEST INFO
		 * =====================================================
		 */
		RequestInfo requestInfo = requestInfoBuilder.build(tenantId);

		request.setRequestInfo(requestInfo);

		/*
		 * ===================================================== BUILD CONTRACTOR
		 * =====================================================
		 */
		Contractor contractor = buildContractor(record);

		/*
		 * ===================================================== ADD CONTRACTOR TO
		 * REQUEST =====================================================
		 */
		request.setBankName(record.getBankName());
		request.setBranchName(record.getBranchName());
		request.setStatusId(106);
		request.setContractor(contractor);

		return request;
	}

	/**
	 * Convert ContractorRecord into Finance API Contractor DTO.
	 */
	private Contractor buildContractor(ContractorRecord record) {

		Contractor contractor = new Contractor();

		/*
		 * ===================================================== BASIC INFORMATION
		 * =====================================================
		 */

		contractor.setCode(record.getCode());

		contractor.setName(record.getName());

		contractor.setCorrespondenceAddress(record.getCorrespondenceAddress());

		contractor.setPaymentAddress(record.getPaymentAddress());

		contractor.setContactPerson(record.getContactPerson());

		contractor.setEmail(record.getEmail());

		contractor.setNarration(record.getNarration());

		/*
		 * ===================================================== PAN / GST
		 * =====================================================
		 */

		contractor.setPanNumber(record.getPanNumber());

		contractor.setTinNumber(record.getTinNumber());

		contractor.setGstReason(record.getGstReason());

		contractor.setPanReason(record.getPanReason());

		/*
		 * ===================================================== CONTACT
		 * =====================================================
		 */

		contractor.setMobileNumber(record.getMobileNumber());

		/*
		 * ===================================================== BANK INFORMATION
		 * =====================================================
		 */

		contractor.setIfscCode(record.getIfscCode());

		contractor.setBankAccount(record.getBankAccount());

		/*
		 * If your Contractor API expects Bank object, you need to resolve the bank
		 * separately.
		 */

		/*
		 * ===================================================== CONTRACTOR INFORMATION
		 * =====================================================
		 */

		contractor.setSource(record.getSource());

		contractor.setRegistrationNumber(record.getRegistrationNumber());

		contractor.setEpfNumber(record.getEpfNumber());

		contractor.setEsiNumber(record.getEsiNumber());

		contractor.setGstRegisteredState(record.getGstRegisteredState());

		contractor.setContractorType(record.getContractorType());

		/*
		 * ===================================================== STATUS
		 * =====================================================
		 *
		 * IMPORTANT:
		 *
		 * Do NOT do:
		 *
		 * EgwStatus status = new EgwStatus(); status.setId(record.getStatusId());
		 * contractor.setStatus(status);
		 *
		 * if the Finance service expects a managed EgwStatus.
		 *
		 * Resolve the status in the Finance service.
		 */

		return contractor;
	}
}