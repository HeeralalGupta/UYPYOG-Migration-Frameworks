package org.egov.finance.migration.service;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.factory.MigrationProcessorFactory;
import org.egov.finance.migration.processor.MigrationProcessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MigrationAsyncService {

	private final MigrationProcessorFactory factory;
	private final MigrationCancellationManager cancellationManager;

	public MigrationAsyncService(MigrationProcessorFactory factory, MigrationCancellationManager cancellationManager) {

		this.factory = factory;
		this.cancellationManager = cancellationManager;
	}

	@Async("migrationExecutor")
	public void processAsync(MigrationRequest request) {
		
		log.info("Migration async STARTED: jobId={}", request.getJobId());

		try {

			MigrationProcessor processor = factory.getProcessor(request.getMigrationType());
			processor.process(request);
			log.info("Migration async COMPLETED: jobId={}", request.getJobId());

		} catch (Exception e) {
			log.error("Migration async FAILED: jobId={}", request.getJobId(), e);
		}finally {
			
		    // Remove cancellation flag after job finishes
		    cancellationManager.remove(request.getJobId());
			
            if (request.getFilePath() != null) {
                try {
                    Files.deleteIfExists(
                        Paths.get(request.getFilePath())
                    );

                    log.info(
                        "Temporary migration file deleted: jobId={}, path={}",
                        request.getJobId(),
                        request.getFilePath()
                    );

                } catch (Exception e) {
                    log.warn(
                        "Could not delete temporary migration file: jobId={}, path={}",
                        request.getJobId(),
                        request.getFilePath(),
                        e
                    );
                }
            }

            log.info("Migration async ENDED: jobId={}", request.getJobId());
        }
	}
}