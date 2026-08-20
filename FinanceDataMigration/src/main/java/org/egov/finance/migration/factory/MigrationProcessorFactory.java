package org.egov.finance.migration.factory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.processor.MigrationProcessor;
import org.springframework.stereotype.Component;

@Component
public class MigrationProcessorFactory {

	private final Map<MigrationType, MigrationProcessor> processorMap = new EnumMap<>(MigrationType.class);

	public MigrationProcessorFactory(List<MigrationProcessor> processors) {
		for (MigrationProcessor processor : processors) {
			processorMap.put(processor.getMigrationType(), processor);
		}
	}

	public MigrationProcessor getProcessor(MigrationType migrationType) {

		MigrationProcessor processor = processorMap.get(migrationType);
		if (processor == null) {
			throw new IllegalArgumentException("No processor found for : " + migrationType);
		}
		return processor;
	}

}