package org.egov.finance.migration.controller;

import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.service.BankAccountFileValidationService;
import org.egov.finance.migration.service.BankBranchFileValidationService;
import org.egov.finance.migration.service.BankFileValidationService;
import org.egov.finance.migration.service.ContractorFileValidationService;
import org.egov.finance.migration.service.FileValidationService;
import org.egov.finance.migration.service.FundFileValidationService;
import org.egov.finance.migration.service.PurchaseOrderFileValidationService;
import org.egov.finance.migration.service.SchemeFileValidationService;
import org.egov.finance.migration.service.SupplierFileValidationService;
import org.egov.finance.migration.service.WorkFileValidationService;
import org.egov.finance.migration.service.WorkOrderFileValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/migration")
public class FileValidationController {

	private final FileValidationService validationService;
	private final FundFileValidationService fundFileValidationService;
	private final SchemeFileValidationService schemeFileValidationService;
	private final BankFileValidationService bankFileValidationService;
	private final BankBranchFileValidationService bankBranchFileValidationService;
	private final BankAccountFileValidationService bankAccountFileValidationService;
	private final ContractorFileValidationService contractorFileValidationService;
	private final SupplierFileValidationService supplierFileValidationService;
	private final WorkFileValidationService workFileValidationService;
	private final WorkOrderFileValidationService workOrderFileValidationService;
	private final PurchaseOrderFileValidationService purchaseOrderFileValidationService;

	public FileValidationController(FileValidationService validationService,
			FundFileValidationService fundFileValidationService,
			SchemeFileValidationService schemeFileValidationService,
			BankFileValidationService bankFileValidationService,
			BankBranchFileValidationService bankBranchFileValidationService,
			BankAccountFileValidationService bankAccountFileValidationService,
			ContractorFileValidationService contractorFileValidationService,
			SupplierFileValidationService supplierFileValidationService,
			WorkFileValidationService workFileValidationService,
			WorkOrderFileValidationService workOrderFileValidationService,
			PurchaseOrderFileValidationService purchaseOrderFileValidationService) {

		this.validationService = validationService;
		this.fundFileValidationService = fundFileValidationService;
		this.schemeFileValidationService = schemeFileValidationService;
		this.bankFileValidationService = bankFileValidationService;
		this.bankBranchFileValidationService = bankBranchFileValidationService;
		this.bankAccountFileValidationService = bankAccountFileValidationService;
		this.contractorFileValidationService = contractorFileValidationService;
		this.supplierFileValidationService = supplierFileValidationService;
		this.workFileValidationService = workFileValidationService;
		this.workOrderFileValidationService = workOrderFileValidationService;
		this.purchaseOrderFileValidationService = purchaseOrderFileValidationService;

	}

	@PostMapping(value = "/validate/{module}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateFile(@PathVariable String module, @RequestParam("file") MultipartFile file) {
		System.out.println("Module Name is : " + module);
		return validationService.validate(file, module);
	}

	@PostMapping(value = "/validate/FUND", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateFundFile(@RequestParam("file") MultipartFile file) {
		return fundFileValidationService.validate(file);
	}
    
	@PostMapping(value = "/validate/SCHEME", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateSchemeFile(@RequestParam("file") MultipartFile file) {
		return schemeFileValidationService.validate(file);
	}
	
	@PostMapping(value = "/validate/BANK", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateBankFile(@RequestParam("file") MultipartFile file) {
		return bankFileValidationService.validate(file);
	}
	
	@PostMapping(value = "/validate/BANK_BRANCH", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateBankBranchFile(@RequestParam("file") MultipartFile file) {
		return bankBranchFileValidationService.validate(file);
	}
	
	@PostMapping(value = "/validate/BANK_ACCOUNT", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateBankAccountFile(@RequestParam("file") MultipartFile file) {
		return bankAccountFileValidationService.validate(file);
	}
	
	@PostMapping(value = "/validate/CONTRACTOR", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateContractorFile(@RequestParam("file") MultipartFile file) {
		return contractorFileValidationService.validate(file);
	}
	
	@PostMapping(value = "/validate/WORK", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateWorkFile(@RequestParam("file") MultipartFile file) {
		return workFileValidationService.validate(file);
	}
    
	@PostMapping(value = "/validate/SUPPLIER", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateSupplierFile(@RequestParam("file") MultipartFile file) {
		return supplierFileValidationService.validate(file);
	}
	
	@PostMapping(value = "/validate/WORK_ORDER", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateWorkOrderFile(@RequestParam("file") MultipartFile file) {
		return workOrderFileValidationService.validate(file);
	}

	@PostMapping(value = "/validate/PURCHASE_ORDER", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validatePurchaseOrderFile(@RequestParam("file") MultipartFile file) {
		return purchaseOrderFileValidationService.validate(file);
	}
}