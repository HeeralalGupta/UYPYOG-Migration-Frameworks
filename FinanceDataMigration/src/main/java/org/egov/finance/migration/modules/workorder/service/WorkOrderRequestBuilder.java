package org.egov.finance.migration.modules.workorder.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.finance.migration.modules.workorder.dto.WorkOrderItemRecord;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderItemsRequest;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderRecord;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderRequest;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderRequestBuilder {

    /**
     * Build WorkOrderRequest from one WorkOrderRecord.
     */
    public WorkOrderRequest build(WorkOrderRecord record) {

        WorkOrderRequest request = new WorkOrderRequest();

        /*
         * Work Order information
         */
        request.setName(record.getWorkOrderName());
        request.setOrderDate(record.getWorkOrderDate());
        request.setDescription(record.getDescription());
        request.setOrderType(record.getWorkOrderType());
        request.setActive(parseActive(record.getActive()));

        /*
         * Contractor and Work information
         */
        request.setContractorName(record.getContractorName());
        request.setWorkName(record.getWorkName());
        request.setWorkCode(record.getWorkCode());

        /*
         * Financial information
         */
        request.setOrderValue(record.getTotalOrderAmt());
        request.setAdvancePayable(record.getAdvancePayable());

        /*
         * Accounting classification
         */
        request.setFundName(record.getFund());
        request.setDepartmentName(record.getDepartment());
        request.setSchemeName(record.getScheme());
        request.setSubSchemeName(record.getSubScheme());

        /*
         * Approval information
         */
        request.setSanctionNumber(record.getWorkOrderIssuingAuthority());
        request.setSanctionDate(record.getSanctionDate());

        /*
         * Security / Deposit information
         */
        request.setEmdAmount(record.getEmdAmount());
        request.setBgAmount(record.getBgAmount());
        request.setApbg(record.getApbgAmount());

        /*
         * Work Order Items
         */
        request.setWorkItems(buildWorkOrderItems(record.getItems()));

        return request;
    }

    /**
     * Build WorkOrderItemsRequest list.
     */
    private List<WorkOrderItemsRequest> buildWorkOrderItems(
            List<WorkOrderItemRecord> items) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(this::buildWorkOrderItem)
                .collect(Collectors.toList());
    }

    /**
     * Build one WorkOrderItemsRequest.
     */
    private WorkOrderItemsRequest buildWorkOrderItem(
            WorkOrderItemRecord item) {

        WorkOrderItemsRequest request = new WorkOrderItemsRequest();

        request.setItemCode(item.getItemName());
        request.setUnit(item.getUnit());
        request.setUnitRate(item.getUnitRate());
        request.setQuantity(item.getQuantity());

        request.setGstRate(
                item.getGst() != null
                        ? item.getGst().longValue()
                        : null
        );

        request.setUnitValueWithGst(item.getUnitValueWithGst());
        request.setAmount(item.getAmount());
        request.setOrderNumber(item.getWorkOrderNo());

        // glcodeid intentionally skipped

        return request;
    }

    /**
     * Convert active value from Excel to Boolean.
     */
    private Boolean parseActive(String active) {

        if (active == null || active.trim().isEmpty()) {
            return true;
        }

        return "Active".equalsIgnoreCase(active.trim());
    }
}