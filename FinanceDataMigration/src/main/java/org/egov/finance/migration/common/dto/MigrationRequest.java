package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.common.enums.MigrationType;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationRequest {

    private MultipartFile file;
    private String tenantId;
    private MigrationType migrationType;
    private String uploadedBy;
    private String jobId;
}
