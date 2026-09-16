package org.egov.finance.migration.modules.workorder.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderItemRecord;
import org.egov.finance.migration.modules.workorder.dto.WorkOrderRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkOrderExcelReader {

    private final DataFormatter formatter = new DataFormatter();

    public List<WorkOrderRecord> read(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            /*
             * Find sheets by sheet name.
             */
        	Sheet masterSheet = null;
        	Sheet itemsSheet = null;

        	for (Sheet sheet : workbook) {
        	    if (sheet.getSheetName().equalsIgnoreCase(ExcelConstants.WORK_ORDER_MASTER_SHEET)) {
        	        masterSheet = sheet;
        	    }

        	    if (sheet.getSheetName().equalsIgnoreCase(ExcelConstants.WORK_ORDER_ITEMS_SHEET)) {
        	        itemsSheet = sheet;
        	    }
        	}

            validateSheets(masterSheet, itemsSheet);

            /*
             * Read Work Order Master.
             */
            List<WorkOrderRecord> workOrders =
                    readWorkOrderMaster(masterSheet);

            /*
             * Read Work Order Items.
             */
            List<WorkOrderItemRecord> items =
                    readWorkOrderItems(itemsSheet);

            /*
             * Match items with Work Order Master
             * using only Work Order No.
             */
            attachItems(workOrders, items);

            return workOrders;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Unable to read Work Order Excel file.", e);
        }
    }

    /**
     * Read Work Order Master sheet.
     */
    private List<WorkOrderRecord> readWorkOrderMaster(
            Sheet sheet) {

        List<WorkOrderRecord> records =
                new ArrayList<>();

        int headerRowIndex = findMasterHeaderRow(sheet);

        Map<String, Integer> headerMap =
                buildHeaderMap(sheet.getRow(headerRowIndex));

        for (int rowIndex = headerRowIndex + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            WorkOrderRecord record =
                    createWorkOrderRecord(row, headerMap);

            /*
             * Actual Excel row number.
             */
            record.setRowNumber(rowIndex + 1);

            /*
             * Initialize items.
             */
            record.setItems(new ArrayList<>());

            records.add(record);
        }

        return records;
    }

    /**
     * Create Work Order Master record using dynamic columns.
     */
    private WorkOrderRecord createWorkOrderRecord(
            Row row,
            Map<String, Integer> headerMap) {

        WorkOrderRecord record =
                new WorkOrderRecord();

        /*
         * Excel columns:
         *
         * A -> Sl. No.
         * B -> ULB Name
         * C -> Tender Number
         * D -> Work Order No.
         * E -> Work Order Date
         * F -> Work Order Name
         * G -> Work Order Type
         * H -> Description
         * I -> Active
         * J -> Contractor Name
         * K -> Work Name
         * L -> Work Code
         * M -> Total Order Amt
         * N -> Advance Payable
         * O -> Fund
         * P -> Department
         * Q -> Scheme
         * R -> Sub Scheme
         * S -> Work Order Issuing Authority
         * T -> Sanction Date
         * U -> EMD Amount
         * V -> BG Amount
         * W -> APBG Amount
         */

        record.setUlbName(
                getCellValue(row, headerMap, "ulbname"));

        record.setTenderNumber(
                getCellValue(row, headerMap, "tendernumber"));

        record.setWorkOrderNo(
                getCellValue(row, headerMap, "workorderno"));

        record.setWorkOrderDate(
                parseDate(
                        getCell(
                                row,
                                headerMap,
                                "workorderdate")));

        record.setWorkOrderName(
                getCellValue(row, headerMap, "workordername"));

        record.setWorkOrderType(
                getCellValue(row, headerMap, "workordertype"));

        record.setDescription(
                getCellValue(row, headerMap, "description"));

        record.setActive(
                getCellValue(row, headerMap, "active"));

        record.setContractorName(
                getCellValue(row, headerMap, "contractorname"));

        record.setWorkName(
                getCellValue(row, headerMap, "workname"));

        record.setWorkCode(
                getCellValue(row, headerMap, "workcode"));

        record.setTotalOrderAmt(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "totalorderamt")));

        record.setAdvancePayable(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "advancepayable")));

        record.setFund(
                getCellValue(row, headerMap, "fund"));

        record.setDepartment(
                getCellValue(row, headerMap, "department"));

        record.setScheme(
                getCellValue(row, headerMap, "scheme"));

        record.setSubScheme(
                getCellValue(row, headerMap, "subscheme"));

        record.setWorkOrderIssuingAuthority(
                getCellValue(
                        row,
                        headerMap,
                        "workorderissuingauthority"));

        record.setSanctionDate(
                parseDate(
                        getCell(
                                row,
                                headerMap,
                                "sanctiondate")));

        record.setEmdAmount(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "emdamount")));

        record.setBgAmount(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "bgamount")));

        record.setApbgAmount(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "apbgamount")));

        return record;
    }

    /**
     * Read Work Order Items sheet.
     */
    private List<WorkOrderItemRecord> readWorkOrderItems(
            Sheet sheet) {

        List<WorkOrderItemRecord> records =
                new ArrayList<>();

        int headerRowIndex = findItemHeaderRow(sheet);

        Map<String, Integer> headerMap =
                buildHeaderMap(sheet.getRow(headerRowIndex));

        for (int rowIndex = headerRowIndex + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            WorkOrderItemRecord record =
                    createWorkOrderItemRecord(
                            row,
                            headerMap);

            /*
             * Actual Excel row number.
             */
            record.setRowNumber(rowIndex + 1);

            records.add(record);
        }

        return records;
    }

    /**
     * Create Work Order Item record using dynamic columns.
     */
    private WorkOrderItemRecord createWorkOrderItemRecord(
            Row row,
            Map<String, Integer> headerMap) {

        WorkOrderItemRecord record =
                new WorkOrderItemRecord();

        /*
         * Excel columns:
         *
         * A -> Sl. No.
         * B -> Tender Number
         * C -> Work Order No.
         * D -> Item Name
         * E -> GL Code
         * F -> Unit
         * G -> Unit Rate
         * H -> GST %
         * I -> Unit Value with GST
         * J -> Quantity
         * K -> Amount
         */

        record.setTenderNumber(
                getCellValue(
                        row,
                        headerMap,
                        "tendernumber"));

        record.setWorkOrderNo(
                getCellValue(
                        row,
                        headerMap,
                        "workorderno"));

        record.setItemName(
                getCellValue(
                        row,
                        headerMap,
                        "itemname"));

        record.setGlCode(
                getCellValue(
                        row,
                        headerMap,
                        "glcode"));

        record.setUnit(
                getCellValue(
                        row,
                        headerMap,
                        "unit"));

        record.setUnitRate(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "unitrate")));

        record.setGst(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "gst")));

        record.setUnitValueWithGst(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "unitvaluewithgst")));

        record.setQuantity(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "quantity")));

        record.setAmount(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "amount")));

        return record;
    }

    /**
     * Find Master header row dynamically.
     */
    private int findMasterHeaderRow(Sheet sheet) {

        String[] headers = {
                "ulbname",
                "tendernumber",
                "workorderno",
                "workorderdate",
                "workordername",
                "workordertype",
                "contractorname",
                "workname",
                "workcode",
                "fund"
        };

        return findHeaderRow(sheet, headers);
    }

    /**
     * Find Items header row dynamically.
     */
    private int findItemHeaderRow(Sheet sheet) {

        String[] headers = {
                "tendernumber",
                "workorderno",
                "itemname",
                "glcode",
                "unit",
                "unitrate",
                "gst",
                "unitvaluewithgst",
                "quantity",
                "amount"
        };

        return findHeaderRow(sheet, headers);
    }

    /**
     * Find header row by matching known headers.
     */
    private int findHeaderRow(
            Sheet sheet,
            String[] knownHeaders) {

        for (int rowIndex = 0;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            int matchedHeaders = 0;

            for (Cell cell : row) {

                String header =
                        normalizeHeader(
                                formatter.formatCellValue(cell));

                for (String knownHeader : knownHeaders) {

                    if (knownHeader.equals(header)) {
                        matchedHeaders++;
                        break;
                    }
                }
            }

            if (matchedHeaders >= 5) {
                return rowIndex;
            }
        }

        throw new IllegalArgumentException(
                "Excel header row not found.");
    }

    /**
     * Build header -> column index mapping.
     */
    private Map<String, Integer> buildHeaderMap(
            Row headerRow) {

        Map<String, Integer> headerMap =
                new HashMap<>();

        for (Cell cell : headerRow) {

            String header =
                    normalizeHeader(
                            formatter.formatCellValue(cell));

            if (!header.isEmpty()) {

                headerMap.put(
                        header,
                        cell.getColumnIndex());
            }
        }

        return headerMap;
    }

    /**
     * Normalize Excel header.
     *
     * Example:
     * "Work Order No." -> "workorderno"
     */
    private String normalizeHeader(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Get cell using dynamic header mapping.
     */
    private Cell getCell(
            Row row,
            Map<String, Integer> headerMap,
            String header) {

        Integer columnIndex =
                headerMap.get(header);

        if (columnIndex == null) {
            return null;
        }

        return row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
    }

    /**
     * Get cell value using dynamic header mapping.
     */
    private String getCellValue(
            Row row,
            Map<String, Integer> headerMap,
            String header) {

        Cell cell =
                getCell(
                        row,
                        headerMap,
                        header);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    /**
     * Match Work Order Items with Work Order Master.
     */
    private void attachItems(
            List<WorkOrderRecord> workOrders,
            List<WorkOrderItemRecord> items) {

        Map<String, WorkOrderRecord> workOrderMap =
                new HashMap<>();

        /*
         * Create lookup using Work Order No.
         */
        for (WorkOrderRecord workOrder : workOrders) {

            String workOrderNo =
                    normalize(
                            workOrder.getWorkOrderNo());

            if (workOrderNo.isEmpty()) {
                continue;
            }

            workOrderMap.put(
                    workOrderNo,
                    workOrder);
        }

        /*
         * Attach each item to its parent.
         */
        for (WorkOrderItemRecord item : items) {

            String workOrderNo =
                    normalize(
                            item.getWorkOrderNo());

            WorkOrderRecord workOrder =
                    workOrderMap.get(workOrderNo);

            if (workOrder == null) {

                throw new IllegalArgumentException(
                        "Work Order not found for item at Excel row "
                                + item.getRowNumber()
                                + ". Work Order No: "
                                + item.getWorkOrderNo());
            }

            workOrder.getItems().add(item);
        }
    }

    /**
     * Normalize value before matching.
     */
    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase();
    }

    /**
     * Validate required sheets.
     */
    private void validateSheets(
            Sheet masterSheet,
            Sheet itemsSheet) {

        if (masterSheet == null) {

            throw new IllegalArgumentException(
                    "Required sheet not found: "
                            + ExcelConstants.WORK_ORDER_MASTER_SHEET);
        }

        if (itemsSheet == null) {

            throw new IllegalArgumentException(
                    "Required sheet not found: "
                            + ExcelConstants.WORK_ORDER_ITEMS_SHEET);
        }
    }

    private BigDecimal parseBigDecimal(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return null;
        }

        try {

            return new BigDecimal(
                    value.replace(",", "").trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid numeric value: " + value,
                    e);
        }
    }

    /**
     * Parse Excel date.
     */
    private Date parseDate(Cell cell) {

        if (cell == null ||
                cell.getCellType() == CellType.BLANK) {

            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {

            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }

            return DateUtil.getJavaDate(
                    cell.getNumericCellValue());
        }

        if (cell.getCellType() == CellType.FORMULA
                && DateUtil.isCellDateFormatted(cell)) {

            return cell.getDateCellValue();
        }

        String value =
                formatter
                        .formatCellValue(cell)
                        .trim();

        if (value.isEmpty()) {
            return null;
        }

        String[] formats = {
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "dd.MM.yyyy",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "yyyy.MM.dd",
                "MM/dd/yyyy",
                "MM-dd-yyyy",
                "MM.dd.yyyy",
                "dd/MM/yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy HH:mm",
                "dd-MM-yyyy HH:mm",
                "yyyy-MM-dd HH:mm"
        };

        for (String format : formats) {

            try {

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(format);

                dateFormat.setLenient(false);

                return dateFormat.parse(value);

            } catch (ParseException ignored) {
                // Try next format.
            }
        }

        throw new IllegalArgumentException(
                "Invalid date value: " + value);
    }

    /**
     * Check whether complete row is empty.
     */
    private boolean isEmptyRow(Row row) {

        for (Cell cell : row) {

            if (!formatter
                    .formatCellValue(cell)
                    .trim()
                    .isEmpty()) {

                return false;
            }
        }

        return true;
    }
}