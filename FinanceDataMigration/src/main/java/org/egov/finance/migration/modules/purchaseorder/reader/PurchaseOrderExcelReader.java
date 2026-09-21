package org.egov.finance.migration.modules.purchaseorder.reader;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderItemRecord;
import org.egov.finance.migration.modules.purchaseorder.dto.PurchaseOrderRecord;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PurchaseOrderExcelReader {

    private final DataFormatter formatter = new DataFormatter();

    public List<PurchaseOrderRecord> read(String filePath) {

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            /*
             * Find sheets by sheet name.
             */
        	Sheet masterSheet = null;
        	Sheet itemsSheet = null;

        	for (Sheet sheet : workbook) {
        	    if (sheet.getSheetName().equalsIgnoreCase(
        	            ExcelConstants.PURCHASE_ORDER_MASTER_SHEET)) {
        	        masterSheet = sheet;
        	    }

        	    if (sheet.getSheetName().equalsIgnoreCase(
        	            ExcelConstants.PURCHASE_ORDER_ITEMS_SHEET)) {
        	        itemsSheet = sheet;
        	    }
        	}

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

        int headerRowIndex =
                findMasterHeaderRow(sheet);

        Map<String, Integer> headerMap =
                buildHeaderMap(
                        sheet.getRow(headerRowIndex));

        for (int rowIndex = headerRowIndex + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            PurchaseOrderRecord record =
                    createPurchaseOrderRecord(
                            row,
                            headerMap);

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
     * Create Purchase Order Master record
     * using dynamic column positions.
     */
    private PurchaseOrderRecord createPurchaseOrderRecord(
            Row row,
            Map<String, Integer> headerMap) {

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
                getCellValue(
                        row,
                        headerMap,
                        "ulbname"));

        record.setOrderNo(
                getCellValue(
                        row,
                        headerMap,
                        "orderno"));

        record.setOrderDate(
                parseDate(
                        getCell(
                                row,
                                headerMap,
                                "orderdate")));

        record.setOrderName(
                getCellValue(
                        row,
                        headerMap,
                        "ordername"));

        record.setDescription(
                getCellValue(
                        row,
                        headerMap,
                        "description"));

        record.setSupplierName(
                getCellValue(
                        row,
                        headerMap,
                        "suppliername"));

        record.setFund(
                getCellValue(
                        row,
                        headerMap,
                        "sourceoffund"));

        record.setDepartment(
                getCellValue(
                        row,
                        headerMap,
                        "department"));

        record.setScheme(
                getCellValue(
                        row,
                        headerMap,
                        "scheme"));

        record.setSubScheme(
                getCellValue(
                        row,
                        headerMap,
                        "subscheme"));

        record.setSanctionNo(
                getCellValue(
                        row,
                        headerMap,
                        "sanctionno"));

        record.setSanctionDate(
                parseDate(
                        getCell(
                                row,
                                headerMap,
                                "sanctiondate")));

        record.setAdvancePayable(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "advancepayable")));

        record.setTotalOrderValue(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "totalordervalue")));

        return record;
    }

    /**
     * Read Purchase Order Items sheet.
     */
    private List<PurchaseOrderItemRecord> readPurchaseOrderItems(
            Sheet sheet) {

        List<PurchaseOrderItemRecord> records =
                new ArrayList<>();

        int headerRowIndex =
                findItemHeaderRow(sheet);

        Map<String, Integer> headerMap =
                buildHeaderMap(
                        sheet.getRow(headerRowIndex));

        for (int rowIndex = headerRowIndex + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row == null || isEmptyRow(row)) {
                continue;
            }

            PurchaseOrderItemRecord record =
                    createPurchaseOrderItemRecord(
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
     * Create Purchase Order Item record
     * using dynamic column positions.
     */
    private PurchaseOrderItemRecord createPurchaseOrderItemRecord(
            Row row,
            Map<String, Integer> headerMap) {

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
                getCellValue(
                        row,
                        headerMap,
                        "ulbname"));

        record.setOrderNo(
                getCellValue(
                        row,
                        headerMap,
                        "orderno"));

        record.setItemDescription(
                getCellValue(
                        row,
                        headerMap,
                        "itemdescription"));

        record.setUnit(
                getCellValue(
                        row,
                        headerMap,
                        "unit"));

        record.setRate(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "rate")));

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
                                "qty")));

        record.setNetAmount(
                parseBigDecimal(
                        getCellValue(
                                row,
                                headerMap,
                                "netamount")));

        return record;
    }

    /**
     * Find Purchase Order Master header row dynamically.
     */
    private int findMasterHeaderRow(Sheet sheet) {

        String[] knownHeaders = {
                "ulbname",
                "orderno",
                "orderdate",
                "ordername",
                "suppliername",
                "sourceoffund",
                "department",
                "scheme",
                "subscheme",
                "sanctionno",
                "sanctiondate",
                "advancepayable",
                "totalordervalue"
        };

        return findHeaderRow(sheet, knownHeaders);
    }

    /**
     * Find Purchase Order Items header row dynamically.
     */
    private int findItemHeaderRow(Sheet sheet) {

        String[] knownHeaders = {
                "ulbname",
                "orderno",
                "itemdescription",
                "unit",
                "rate",
                "gst",
                "unitvaluewithgst",
                "qty",
                "netamount"
        };

        return findHeaderRow(sheet, knownHeaders);
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

            /*
             * At least 5 known headers are required
             * to identify the header row.
             */
            if (matchedHeaders >= 5) {
                return rowIndex;
            }
        }

        throw new IllegalArgumentException(
                "Purchase Order header row not found.");
    }

    /**
     * Build header name -> column index mapping.
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
     * Examples:
     *
     * "ULB Name"              -> "ulbname"
     * "Order No."             -> "orderno"
     * "Order Date"            -> "orderdate"
     * "Source of Fund"        -> "sourceoffund"
     * "Unit Value With GST"   -> "unitvaluewithgst"
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
     * Match Purchase Order Items with
     * their parent Purchase Order.
     */
    private void attachItems(
            List<PurchaseOrderRecord> purchaseOrders,
            List<PurchaseOrderItemRecord> items) {

        Map<String, PurchaseOrderRecord> purchaseOrderMap =
                new HashMap<>();

        for (PurchaseOrderRecord purchaseOrder : purchaseOrders) {

            String orderNo =
                    normalize(
                            purchaseOrder.getOrderNo());

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
                    normalize(
                            item.getOrderNo());

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

        return value
                .trim()
                .toLowerCase();
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
                    value
                            .replace(",", "")
                            .trim());

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

        if (cell == null
                || cell.getCellType() == CellType.BLANK) {

            return null;
        }

        /*
         * Native Excel date / numeric date.
         */
        if (cell.getCellType()
                == CellType.NUMERIC) {

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
         * Formula cell returning date.
         */
        if (cell.getCellType()
                == CellType.FORMULA
                && DateUtil.isCellDateFormatted(cell)) {

            return cell.getDateCellValue();
        }

        /*
         * Date stored as text.
         */
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
                // Try next date format.
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