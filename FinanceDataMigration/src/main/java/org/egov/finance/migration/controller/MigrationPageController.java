package org.egov.finance.migration.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/migration")
public class MigrationPageController {

	@GetMapping({ "", "/" })
	public String home() {
		return "migration/home";
	}

	@GetMapping("/new")
	public String newMigration() {
		return "migration/module-selection";
	}

	@GetMapping("/upload/{module}")
	public String upload(@PathVariable String module, Model model) {

		model.addAttribute("moduleCode", module);

		return "migration/upload";
	}

	@GetMapping("/history")
	public String history() {
		return "migration/history";
	}

	@GetMapping("/reports")
	public String reports() {
		return "migration/reports";
	}

	@GetMapping("/settings")
	public String settings() {
		return "migration/settings";
	}

}
