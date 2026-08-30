package org.egov.finance.migration.service;

import org.egov.finance.migration.common.dto.MigrationRequest;
import org.egov.finance.migration.factory.MigrationProcessorFactory;
import org.egov.finance.migration.processor.MigrationProcessor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MigrationAsyncService {

	private final MigrationProcessorFactory factory;

	public MigrationAsyncService(MigrationProcessorFactory factory) {

		this.factory = factory;
	}

//	@Async
	public void processAsync(MigrationRequest request) {

		try {

			MigrationProcessor processor = factory.getProcessor(request.getMigrationType());
			processor.process(request);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}