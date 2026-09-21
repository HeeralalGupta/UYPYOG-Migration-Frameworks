package org.egov.finance.migration.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component
public class MigrationCancellationManager {

    private final ConcurrentHashMap<String, AtomicBoolean> cancellationFlags =
            new ConcurrentHashMap<>();

    /**
     * Register a migration job for cancellation tracking.
     */
    public void register(String jobId) {

        cancellationFlags.put(
                jobId,
                new AtomicBoolean(false)
        );
    }

    /**
     * Request cancellation for a running migration job.
     */
    public void cancel(String jobId) {

        AtomicBoolean flag = cancellationFlags.get(jobId);

        if (flag != null) {
            flag.set(true);
        }
    }

    /**
     * Check whether cancellation has been requested.
     */
    public boolean isCancelled(String jobId) {

        AtomicBoolean flag = cancellationFlags.get(jobId);

        return flag != null && flag.get();
    }

    /**
     * Remove the job after migration is completed or cancelled.
     */
    public void remove(String jobId) {

        cancellationFlags.remove(jobId);
    }
}