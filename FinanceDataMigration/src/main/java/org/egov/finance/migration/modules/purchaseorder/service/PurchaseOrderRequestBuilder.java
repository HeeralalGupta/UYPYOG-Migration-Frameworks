package org.egov.finance.migration.modules.purchaseorder.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseItemsRequest;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderItemRecord;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRecord;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRequest;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderRequestBuilder {

    /**
     * Build PurchaseOrderRequest from one PurchaseOrderRecord.
     */
    public PurchaseOrderRequest build(PurchaseOrderRecord record) {

        PurchaseOrderRequest request = new PurchaseOrderRequest();

        /*
         * Purchase Order information
         */
        request.setName(record.getOrderName());
        request.setOrderDate(record.getOrderDate());
        request.setDescription(record.getDescription());

        /*
         * Supplier information
         */
        request.setSupplierName(record.getSupplierName());

        /*
         * Financial information
         */
        request.setOrderValue(record.getTotalOrderValue());
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
        request.setSanctionNumber(record.getSanctionNo());
        request.setSanctionDate(record.getSanctionDate());

        /*
         * Purchase Order Items
         */
        request.setPurchaseItems(buildPurchaseOrderItems(record.getItems()));

        return request;
    }

    /**
     * Build PurchaseItemsRequest list.
     */
    private List<PurchaseItemsRequest> buildPurchaseOrderItems(
            List<PurchaseOrderItemRecord> items) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .map(this::buildPurchaseOrderItem)
                .collect(Collectors.toList());
    }

    /**
     * Build one PurchaseItemsRequest.
     */
    private PurchaseItemsRequest buildPurchaseOrderItem(
            PurchaseOrderItemRecord item) {

        PurchaseItemsRequest request = new PurchaseItemsRequest();

        request.setItemCode(item.getItemDescription());
        request.setUnit(item.getUnit());
        request.setUnitRate(item.getRate());
        request.setQuantity(item.getQuantity());

        request.setGstRate(
                item.getGst() != null
                        ? item.getGst().longValue()
                        : null
        );

        request.setUnitValueWithGst(item.getUnitValueWithGst());
        request.setAmount(item.getNetAmount());

        // glcodeid intentionally skipped

        return request;
    }
}