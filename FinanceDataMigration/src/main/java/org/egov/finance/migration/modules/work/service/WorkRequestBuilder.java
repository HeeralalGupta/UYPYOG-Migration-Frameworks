package org.egov.finance.migration.modules.work.service;

import org.egov.finance.migration.modules.work.dto.WorkRecord;
import org.egov.finance.migration.modules.work.dto.WorkRequest;
import org.springframework.stereotype.Service;

@Service
public class WorkRequestBuilder {


    /**
     * Build WorkRequest from one WorkRecord.
     */
    public WorkRequest build(WorkRecord record) {

        WorkRequest request = new WorkRequest();
        
        /*
         * Work information
         */
        request.setWorkName(record.getNameOfWork());
        request.setWorkType(record.getWorkType());
        request.setFundName(record.getFund());
        request.setStartDate(record.getStartDate());
        request.setEndDate(record.getEndDate());
        request.setEstimateValue(record.getEstimateValue());

        return request;
    }
}