package org.egov.finance.migration.common.dto;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
public class CFunction {
    private Long id;
    private String name;
    private String code;
    private String type;
    private int llevel;
    private Boolean isActive;
    private Boolean isNotLeaf;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentId")
    private CFunction parentId;
}
