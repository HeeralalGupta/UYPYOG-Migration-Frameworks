package org.egov.finance.migration.modules.purchaseorder.reader;

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
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderItemRecord;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRecord;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PurchaseOrderExcelReader {

    private static final int DATA_START_ROW = 3;

    private final DataFormatter formatter = new DataFormatter();

    public List<PurchaseOrderRecord> read(MultipartFile file) {

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            /*
             * Find sheets by sheet name.
             */
            Sheet masterSheet =
                    workbook.getSheet(
                            ExcelConstants.PURCHASE_ORDER_MASTER_SHEET);

            Sheet itemsSheet =
                    workbook.getSheet(
                            ExcelConstants.PURCHASE_ORDER_ITEMS_SHEET);

            validateSheets(masterSheet, itemsSheet);

            /*
             * Read Purchase Order Master.
             */
            List<PurchaseOrderRecord> purchaseOrders =
                    readPurchaseOrderMaster(masterSheet);

            /*
             * Read Purchase Order Items.
             */
            List<PurchaseOrderItemRecord> items =
                    readPurchaseOrderItems(itemsSheet);

            /*
             * Match items with Purchase Order Master
             * using only Order No.
             */
            attachItems(purchaseOrders, items);

            return purchaseOrders;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to read Purchase Order Excel file.", e);
        }
    }

    /**
     * Read Purchase Order Master sheet.
     */
    private List<PurchaseOrderRecord> readPurchaseOrderMaster(
            Sheet sheet) {

        List<PurchaseOrderRecord> records =
                new ArrayList<>();

        for (int rowIndex = DATA_START_ROW;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            PurchaseOrderRecord record =
                    createPurchaseOrderRecord(row);

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
     * Convert one Purchase Order Master row
     * into PurchaseOrderRecord.
     */
    private PurchaseOrderRecord createPurchaseOrderRecord(
            Row row) {

        PurchaseOrderRecord record =
                new PurchaseOrderRecord();

        /*
         * Excel columns:
         *
         * A -> Sl. No.
         * B -> ULB Name
         * C -> Order No.
         * D -> Order Date
         * E -> Order Name
         * F -> Description
         * G -> Supplier Name
         * H -> Fund
         * I -> Department
         * J -> Scheme
         * K -> Sub Scheme
         * L -> Sanction No.
         * M -> Sanction Date
         * N -> Advance Payable
         * O -> Total Order Value
         */

        record.setUlbName(
                getCellValue(row, 1));

        record.setOrderNo(
                getCellValue(row, 2));

        record.setOrderDate(
                parseDate(row.getCell(3)));

        record.setOrderName(
                getCellValue(row, 4));

        record.setDescription(
                getCellValue(row, 5));

        record.setSupplierName(
                getCellValue(row, 6));

        record.setFund(
                getCellValue(row, 7));

        record.setDepartment(
                getCellValue(row, 8));

        record.setScheme(
                getCellValue(row, 9));

        record.setSubScheme(
                getCellValue(row, 10));

        record.setSanctionNo(
                getCellValue(row, 11));

        record.setSanctionDate(
                parseDate(row.getCell(12)));

        record.setAdvancePayable(
                parseBigDecimal(
                        getCellValue(row, 13)));

        record.setTotalOrderValue(
                parseBigDecimal(
                        getCellValue(row, 14)));

        return record;
    }

    /**
     * Read Purchase Order Items sheet.
     */
    private List<PurchaseOrderItemRecord> readPurchaseOrderItems(
            Sheet sheet) {

        List<PurchaseOrderItemRecord> records =
                new ArrayList<>();

        for (int rowIndex = DATA_START_ROW;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            PurchaseOrderItemRecord record =
                    createPurchaseOrderItemRecord(row);

            /*
             * Actual Excel row number.
             */
            record.setRowNumber(rowIndex + 1);

            records.add(record);
        }

        return records;
    }

    /**
     * Convert one Purchase Order Items row
     * into PurchaseOrderItemRecord.
     */
    private PurchaseOrderItemRecord createPurchaseOrderItemRecord(
            Row row) {

        PurchaseOrderItemRecord record =
                new PurchaseOrderItemRecord();

        /*
         * Excel columns:
         *
         * A -> Sl. No.
         * B -> ULB Name
         * C -> Order No.
         * D -> Item Description
         * E -> Unit
         * F -> Rate
         * G -> GST %
         * H -> Unit Value With GST
         * I -> Quantity
         * J -> Net Amount
         */

        record.setUlbName(
                getCellValue(row, 1));

        record.setOrderNo(
                getCellValue(row, 2));

        record.setItemDescription(
                getCellValue(row, 3));

        record.setUnit(
                getCellValue(row, 4));

        record.setRate(
                parseBigDecimal(
                        getCellValue(row, 5)));

        record.setGst(
                parseBigDecimal(
                        getCellValue(row, 6)));

        record.setUnitValueWithGst(
                parseBigDecimal(
                        getCellValue(row, 7)));

        record.setQuantity(
                parseBigDecimal(
                        getCellValue(row, 8)));

        record.setNetAmount(
                parseBigDecimal(
                        getCellValue(row, 9)));

        return record;
    }

    /**
     * Match Purchase Order Items with their parent
     * Purchase Order using ONLY Order No.
     */
    private void attachItems(
            List<PurchaseOrderRecord> purchaseOrders,
            List<PurchaseOrderItemRecord> items) {

        Map<String, PurchaseOrderRecord> purchaseOrderMap =
                new HashMap<>();

        /*
         * Create lookup using Order No.
         */
        for (PurchaseOrderRecord purchaseOrder : purchaseOrders) {

            String orderNo =
                    normalize(purchaseOrder.getOrderNo());

            if (orderNo.isEmpty()) {
                continue;
            }

            purchaseOrderMap.put(
                    orderNo,
                    purchaseOrder);
        }

        /*
         * Attach each item to its parent.
         */
        for (PurchaseOrderItemRecord item : items) {

            String orderNo =
                    normalize(item.getOrderNo());

            PurchaseOrderRecord purchaseOrder =
                    purchaseOrderMap.get(orderNo);

            if (purchaseOrder == null) {

                throw new IllegalArgumentException(
                        "Purchase Order not found for item at Excel row "
                                + item.getRowNumber()
                                + ". Order No: "
                                + item.getOrderNo());
            }

            purchaseOrder.getItems().add(item);
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
                            + ExcelConstants.PURCHASE_ORDER_MASTER_SHEET);
        }

        if (itemsSheet == null) {

            throw new IllegalArgumentException(
                    "Required sheet not found: "
                            + ExcelConstants.PURCHASE_ORDER_ITEMS_SHEET);
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

        /*
         * Native Excel date / numeric date
         */
        if (cell.getCellType()
                == org.apache.poi.ss.usermodel.CellType.NUMERIC) {

            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }

            /*
             * Excel numeric date even if formatting
             * is not detected.
             */
            return DateUtil.getJavaDate(
                    cell.getNumericCellValue());
        }

        /*
         * Date stored as text.
         */
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