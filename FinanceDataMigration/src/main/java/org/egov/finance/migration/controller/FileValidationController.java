package org.egov.finance.migration.controller;

import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.service.FileValidationService;
import org.egov.finance.migration.service.FundFileValidationService;
import org.egov.finance.migration.service.PurchaseOrderFileValidationService;
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
	private final WorkFileValidationService workFileValidationService;
	private final WorkOrderFileValidationService workOrderFileValidationService;
	private final PurchaseOrderFileValidationService purchaseOrderFileValidationService;

	public FileValidationController(FileValidationService validationService,
			FundFileValidationService fundFileValidationService,
			WorkFileValidationService workFileValidationService,
			WorkOrderFileValidationService workOrderFileValidationService,
			PurchaseOrderFileValidationService purchaseOrderFileValidationService) {

		this.validationService = validationService;
		this.fundFileValidationService = fundFileValidationService;
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

	@PostMapping(value = "/validate/WORK", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateWorkFile(@RequestParam("file") MultipartFile file) {
		return workFileValidationService.validate(file);
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