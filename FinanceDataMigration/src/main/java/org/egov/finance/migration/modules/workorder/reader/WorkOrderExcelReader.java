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

    private static final int DATA_START_ROW = 3;


    private final DataFormatter formatter = new DataFormatter();

    public List<WorkOrderRecord> read(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            /*
             * Find sheets by sheet name.
             */
            Sheet masterSheet =
                    workbook.getSheet(ExcelConstants.WORK_ORDER_MASTER_SHEET);

            Sheet itemsSheet =
                    workbook.getSheet(ExcelConstants.WORK_ORDER_ITEMS_SHEET);

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

        for (int rowIndex = DATA_START_ROW;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            WorkOrderRecord record =
                    createWorkOrderRecord(row);

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
     * Convert one Work Order Master row
     * into WorkOrderRecord.
     */
    private WorkOrderRecord createWorkOrderRecord(
            Row row) {

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
                getCellValue(row, 1));

        record.setTenderNumber(
                getCellValue(row, 2));

        record.setWorkOrderNo(
                getCellValue(row, 3));

        record.setWorkOrderDate(
                parseDate(row.getCell(4)));

        record.setWorkOrderName(
                getCellValue(row, 5));

        record.setWorkOrderType(
                getCellValue(row, 6));

        record.setDescription(
                getCellValue(row, 7));

        record.setActive(
                getCellValue(row, 8));

        record.setContractorName(
                getCellValue(row, 9));

        record.setWorkName(
                getCellValue(row, 10));

        record.setWorkCode(
                getCellValue(row, 11));

        record.setTotalOrderAmt(
                parseBigDecimal(
                        getCellValue(row, 12)));

        record.setAdvancePayable(
                parseBigDecimal(
                        getCellValue(row, 13)));

        record.setFund(
                getCellValue(row, 14));

        record.setDepartment(
                getCellValue(row, 15));

        record.setScheme(
                getCellValue(row, 16));

        record.setSubScheme(
                getCellValue(row, 17));

        record.setWorkOrderIssuingAuthority(
                getCellValue(row, 18));

        record.setSanctionDate(
                parseDate(row.getCell(19)));

        record.setEmdAmount(
                parseBigDecimal(
                        getCellValue(row, 20)));

        record.setBgAmount(
                parseBigDecimal(
                        getCellValue(row, 21)));

        record.setApbgAmount(
                parseBigDecimal(
                        getCellValue(row, 22)));

        return record;
    }

    /**
     * Read Work Order Items sheet.
     */
    private List<WorkOrderItemRecord> readWorkOrderItems(
            Sheet sheet) {

        List<WorkOrderItemRecord> records =
                new ArrayList<>();

        for (int rowIndex = DATA_START_ROW;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            WorkOrderItemRecord record =
                    createWorkOrderItemRecord(row);

            /*
             * Actual Excel row number.
             */
            record.setRowNumber(rowIndex + 1);

            records.add(record);
        }

        return records;
    }

    /**
     * Convert one Work Order Items row
     * into WorkOrderItemRecord.
     */
    private WorkOrderItemRecord createWorkOrderItemRecord(
            Row row) {

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
                getCellValue(row, 1));

        record.setWorkOrderNo(
                getCellValue(row, 2));

        record.setItemName(
                getCellValue(row, 3));

        record.setGlCode(
                getCellValue(row, 4));

        record.setUnit(
                getCellValue(row, 5));

        record.setUnitRate(
                parseBigDecimal(
                        getCellValue(row, 6)));

        record.setGst(
                parseBigDecimal(
                        getCellValue(row, 7)));

        record.setUnitValueWithGst(
                parseBigDecimal(
                        getCellValue(row, 8)));

        record.setQuantity(
                parseBigDecimal(
                        getCellValue(row, 9)));

        record.setAmount(
                parseBigDecimal(
                        getCellValue(row, 10)));

        return record;
    }

    /**
     * Match Work Order Items with their parent
     * Work Order using ONLY workOrderNo.
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
                    normalize(workOrder.getWorkOrderNo());

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
                    normalize(item.getWorkOrderNo());

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

    /**
     * Read cell value as String.
     */
    private String getCellValue(
            Row row,
            int columnIndex) {

        Cell cell = row.getCell(
                columnIndex,
                Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    /**
     * Parse numeric value into BigDecimal.
     */
    private BigDecimal parseBigDecimal(
            String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return null;
        }

        try {

            return new BigDecimal(
                    value.replace(",", "").trim());

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "Invalid numeric value: "
                            + value,
                    e);
        }
    }

    /**
     * Parse Excel date.
     */
    private Date parseDate(Cell cell) {

        if (cell == null) {
            return null;
        }

        // Native Excel date / numeric date
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }

            // Excel numeric date even if formatting is not detected
            return DateUtil.getJavaDate(cell.getNumericCellValue());
        }

        // Date stored as text
        String value = formatter
                .formatCellValue(cell)
                .trim();

        if (value.isEmpty()) {
            return null;
        }

        String[] formats = {
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd",
                "MM/dd/yyyy"
        };

        for (String format : formats) {

            try {

                SimpleDateFormat dateFormat =
                        new SimpleDateFormat(format);

                dateFormat.setLenient(false);

                return dateFormat.parse(value);

            } catch (ParseException ignored) {
                // Try next format
            }
        }

        throw new IllegalArgumentException(
                "Invalid date value: " + value);
    }

    /**
     * Check whether complete row is empty.
     */
    private boolean isEmptyRow(Row row) {

        for (int i = 0;
             i < row.getLastCellNum();
             i++) {

            if (!getCellValue(row, i).isEmpty()) {
                return false;
            }
        }

        return true;
    }
}