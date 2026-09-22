package org.egov.finance.migration.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.egov.finance.migration.common.dto.MigrationProgress;
import org.egov.finance.migration.common.dto.MigrationRecordResultDTO;
import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.common.dto.MigrationStartResponse;
import org.egov.finance.migration.common.entity.MigrationJob;
import org.egov.finance.migration.common.entity.MigrationJobDetail;
import org.egov.finance.migration.common.repository.MigrationJobDetailRepository;
import org.egov.finance.migration.common.repository.MigrationJobRepository;
import org.egov.finance.migration.service.MigrationAsyncService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


import org.egov.finance.migration.service.MigrationCancellationManager;
import org.egov.finance.migration.service.MigrationProgressPublisher;

@Controller
@RequestMapping("/migration")
public class MigrationController {

	private MigrationJobRepository migrationJobRepository;
	private final MigrationAsyncService migrationAsyncService;
	private final MigrationJobDetailRepository migrationJobDetailRepository;
	private final MigrationCancellationManager cancellationManager;
	private final MigrationProgressPublisher progressPublisher;

	public MigrationController(MigrationJobRepository migrationJobRepository,
			MigrationJobDetailRepository migrationJobDetailRepository, MigrationAsyncService migrationAsyncService, MigrationCancellationManager cancellationManager, MigrationProgressPublisher progressPublisher) {

		this.migrationJobRepository = migrationJobRepository;
		this.migrationJobDetailRepository = migrationJobDetailRepository;
		this.migrationAsyncService = migrationAsyncService;
		this.cancellationManager = cancellationManager;
		this.progressPublisher = progressPublisher;
	}

	@PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseBody
	public MigrationStartResponse process(@ModelAttribute MigrationRequest request) throws IOException{
		
	    List<String> runningStatuses = List.of(
	            "RUNNING",
	            "PROCESSING",
	            "CANCEL_REQUESTED"
	    );

	    if (migrationJobRepository
	            .findFirstByCreatedByAndTenantIdAndModuleCodeAndStatusInOrderByStartedTimeDesc(
	                    request.getUploadedBy(),
	                    request.getTenantId(),
	                    request.getMigrationType().name(),
	                    runningStatuses)
	            .isPresent()) {

	    	throw new IllegalStateException(
	    	        "A migration is already running for this module. "
	    	        + "Please wait until it is completed or cancelled.");
	    }
		// Generate Job ID
		String jobId = UUID.randomUUID().toString();
		request.setJobId(jobId);

		// Create parent job

		MigrationJob job = new MigrationJob();

		job.setJobId(jobId);
		job.setTenantId(request.getTenantId());
		job.setModuleCode(request.getMigrationType().name());
		job.setFileName(request.getFile() != null ? request.getFile().getOriginalFilename() : null);
		job.setCreatedBy(request.getUploadedBy());
		job.setStatus("PROCESSING");
		job.setTotalRecords(0);
		job.setSuccessRecords(0);
		job.setFailedRecords(0);
		job.setSkippedRecords(0);
		job.setCurrentRecord(0);
		job.setProgressPercent(0);
		job.setCurrentMessage("Migration started");
		job.setStartedTime(LocalDateTime.now());
		migrationJobRepository.save(job);
		
		cancellationManager.register(jobId);

		// Save file before async processing
		if (request.getFile() != null && !request.getFile().isEmpty()) {
		    Path tempFile = Files.createTempFile("migration-", ".xlsx");
		    request.getFile().transferTo(tempFile.toFile());
		    request.setFilePath(tempFile.toString());
		}
		
		// Start background processing

		migrationAsyncService.processAsync(request);

		// Immediately return

		return MigrationStartResponse.builder().jobId(jobId).status("PROCESSING")
				.message("Migration started successfully.").build();
	}

	@GetMapping("/progress/{jobId}")
	@ResponseBody
	public MigrationProgress getProgress(@PathVariable String jobId) {

		MigrationJob job = migrationJobRepository.findByJobId(jobId)
				.orElseThrow(() -> new IllegalArgumentException("Migration job not found: " + jobId));

		return MigrationProgress.builder().jobId(job.getJobId()).status(job.getStatus())
				.totalRecords(job.getTotalRecords()).currentRecord(job.getCurrentRecord())
				.progressPercent(job.getProgressPercent()).successRecords(job.getSuccessRecords())
				.failedRecords(job.getFailedRecords()).skippedRecords(job.getSkippedRecords())
				.currentMessage(job.getCurrentMessage()).build();
	}

	@GetMapping("/result/{jobId}")
	@ResponseBody
	public List<MigrationRecordResultDTO> getMigrationResult(@PathVariable String jobId) {

		List<MigrationJobDetail> details = migrationJobDetailRepository.findByJobJobIdOrderByRecordNumberAsc(jobId);

		return details.stream()
				.map(detail -> MigrationRecordResultDTO.builder().recordNumber(detail.getRecordNumber())
						.startRow(detail.getStartRow()).endRow(detail.getEndRow()).status(detail.getStatus())
						.message(detail.getMessage()).executionTime(detail.getExecutionTime())
						.createdTime(detail.getCreatedTime()).build())
				.collect(Collectors.toList());
	}
	
	@PostMapping("/cancel/{jobId}")
	@ResponseBody
	public MigrationProgress cancelMigration(
	        @PathVariable String jobId) {

	    MigrationJob job = migrationJobRepository
	            .findByJobId(jobId)
	            .orElseThrow(() ->
	                    new IllegalArgumentException(
	                            "Migration job not found: " + jobId));

	    if ("COMPLETED".equals(job.getStatus())
	            || "COMPLETED_WITH_ERRORS".equals(job.getStatus())
	            || "FAILED".equals(job.getStatus())
	            || "CANCELLED".equals(job.getStatus())) {

	        throw new IllegalStateException(
	                "Migration is already finished.");
	    }

	    boolean cancellationRequested = cancellationManager.cancel(jobId);

	    if (cancellationRequested) {

	        job.setStatus("CANCEL_REQUESTED");
	        job.setCurrentMessage(
	                "Cancellation requested by user.");

	    } else {

	        job.setStatus("CANCELLED");
	        job.setCurrentMessage(
	                "Migration was not running.");
	    }

	    migrationJobRepository.saveAndFlush(job);

	    progressPublisher.publish(job);

	    return getProgress(jobId);
	}
	
	@GetMapping("/running")
	@ResponseBody
	public MigrationProgress getRunningMigration(
	        @RequestParam String username,
	        @RequestParam String tenantId,
	        @RequestParam String moduleCode) {

	    List<String> runningStatuses = List.of(
	            "RUNNING",
	            "PROCESSING",
	            "CANCEL_REQUESTED"
	    );

	    return migrationJobRepository
	            .findFirstByCreatedByAndTenantIdAndModuleCodeAndStatusInOrderByStartedTimeDesc(
	                    username,
	                    tenantId,
	                    moduleCode,
	                    runningStatuses)
	            .map(job -> MigrationProgress.builder()
	                    .jobId(job.getJobId())
	                    .status(job.getStatus())
	                    .totalRecords(job.getTotalRecords())
	                    .currentRecord(job.getCurrentRecord())
	                    .progressPercent(job.getProgressPercent())
	                    .successRecords(job.getSuccessRecords())
	                    .failedRecords(job.getFailedRecords())
	                    .skippedRecords(job.getSkippedRecords())
	                    .currentMessage(job.getCurrentMessage())
	                    .build())
	            .orElse(null);
	}

}