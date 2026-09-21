package org.egov.finance.migration.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${migration.executor.core-pool-size}")
    private int corePoolSize;

    @Value("${migration.executor.max-pool-size}")
    private int maxPoolSize;

    @Value("${migration.executor.queue-capacity}")
    private int queueCapacity;

    @Value("${migration.executor.thread-name-prefix}")
    private String threadNamePrefix;

    @Value("${migration.executor.wait-for-tasks-to-complete}")
    private boolean waitForTasksToComplete;

    @Value("${migration.executor.await-termination-seconds}")
    private int awaitTerminationSeconds;

    @Bean(name = "migrationExecutor")
    Executor migrationExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        executor.setWaitForTasksToCompleteOnShutdown(
                waitForTasksToComplete
        );

        executor.setAwaitTerminationSeconds(
                awaitTerminationSeconds
        );

        executor.initialize();

        return executor;
    }
}