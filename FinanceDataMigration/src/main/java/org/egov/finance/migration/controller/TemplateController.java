package org.egov.finance.migration.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migration/template")
public class TemplateController {

	@GetMapping("/{module}")
	public ResponseEntity<Resource> downloadTemplate(@PathVariable String module) throws IOException {

		String fileName = module.toUpperCase() + ".xlsx";

		String resourcePath = "/migration-templates/" + fileName;

		InputStream inputStream = getClass().getResourceAsStream(resourcePath);

		if (inputStream == null) {
			return ResponseEntity.notFound().build();
		}

		InputStreamResource resource = new InputStreamResource(inputStream);

		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
				.contentType(
						MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(resource);
	}
}
