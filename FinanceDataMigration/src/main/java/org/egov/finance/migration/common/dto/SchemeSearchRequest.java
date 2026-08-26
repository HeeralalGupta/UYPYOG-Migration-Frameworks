package org.egov.finance.migration.common.dto;

import org.egov.finance.migration.modules.contractorbill.dto.IdReference;

public class SchemeSearchRequest {

    private String code;

    private String name;

    private IdReference fund;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IdReference getFund() {
        return fund;
    }

    public void setFund(IdReference fund) {
        this.fund = fund;
    }
}