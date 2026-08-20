package org.egov.finance.migration.common.entity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "migration_job")
@Data
public class MigrationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private String jobId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "module_code", nullable = false)
    private String moduleCode;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "status")
    private String status;

    @Column(name = "total_records")
    private Integer totalRecords;

    @Column(name = "success_records")
    private Integer successRecords;

    @Column(name = "failed_records")
    private Integer failedRecords;

    @Column(name = "skipped_records")
    private Integer skippedRecords;

    @Column(name = "started_time")
    private LocalDateTime startedTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;
    
    @Column(name = "current_record")
    private Integer currentRecord;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "current_message")
    private String currentMessage;
}
