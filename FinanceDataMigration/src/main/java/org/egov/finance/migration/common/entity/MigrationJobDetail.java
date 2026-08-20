package org.egov.finance.migration.common.entity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(
    name = "migration_job_detail",
    indexes = {
        @Index(
            name = "idx_migration_duplicate",
            columnList =
                "tenant_id,module_code,start_row,end_row,status"
        )
    }
)
@Data
public class MigrationJobDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private MigrationJob job;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "module_code", nullable = false)
    private String moduleCode;

    @Column(name = "record_number")
    private Integer recordNumber;

    @Column(name = "start_row")
    private Integer startRow;

    @Column(name = "end_row")
    private Integer endRow;

    @Column(name = "record_key")
    private String recordKey;

    @Column(name = "status")
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "execution_time")
    private Long executionTime;

    @Column(name = "created_time")
    private LocalDateTime createdTime;
}