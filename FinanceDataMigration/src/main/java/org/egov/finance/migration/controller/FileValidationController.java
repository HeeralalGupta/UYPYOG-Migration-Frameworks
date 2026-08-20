package org.egov.finance.migration.controller;

import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.service.FileValidationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/migration")
public class FileValidationController {

	private final FileValidationService validationService;

	public FileValidationController(FileValidationService validationService) {

		this.validationService = validationService;
	}

	@PostMapping(value = "/validate/{module}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public FileValidationResult validateFile(@PathVariable String module, @RequestParam("file") MultipartFile file) {
		System.out.println("Module Name is : "+module);
		return validationService.validate(file, module);
	}
}