package org.egov.finance.migration.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import org.egov.finance.migration.common.constants.ExcelConstants;
import org.egov.finance.migration.common.dto.FileValidationResult;
import org.egov.finance.migration.common.dto.RowValidationError;
import org.egov.finance.migration.common.enums.MigrationType;
import org.egov.finance.migration.service.validator.PurchaseOrderRowValidator;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PurchaseOrderFileValidationService
        extends AbstractFileValidationService {

    private final PurchaseOrderRowValidator purchaseOrderRowValidator;

    public PurchaseOrderFileValidationService(
            PurchaseOrderRowValidator purchaseOrderRowValidator) {

        super(purchaseOrderRowValidator);
        this.purchaseOrderRowValidator =
                purchaseOrderRowValidator;
    }

    /**
     * =====================================================
     * MODULE CODE
     * =====================================================
     */
    @Override
    protected MigrationType getModuleCode() {
        return MigrationType.PURCHASE_ORDER;
    }

    /**
     * =====================================================
     * MAIN VALIDATION
     *
     * Purchase Order contains two sheets:
     *
     * 1. Purchase Order Master
     * 2. Purchase Order Items
     *
     * =====================================================
     */
    @Override
    public FileValidationResult validate(
            MultipartFile file) {

        FileValidationResult result =
                new FileValidationResult();

        result.setFileName(
                file.getOriginalFilename());

        result.setModuleCode(
                getModuleCode().toString());

        try (InputStream inputStream =
                     file.getInputStream();

             Workbook workbook =
                     WorkbookFactory.create(inputStream)) {

            /*
             * =================================================
             * FORMULA EVALUATOR
             * =================================================
             */
            FormulaEvaluator formulaEvaluator =
                    workbook.getCreationHelper()
                            .createFormulaEvaluator();

            /*
             * =================================================
             * 1. GET PURCHASE ORDER MASTER SHEET
             * =================================================
             */
            Sheet masterSheet =
                    workbook.getSheet(
                            ExcelConstants
                                    .PURCHASE_ORDER_MASTER_SHEET);

            if (masterSheet == null) {

                result.setValid(false);

                result.getErrors().add(
                        "Required sheet not found: "
                                + ExcelConstants
                                .PURCHASE_ORDER_MASTER_SHEET);

                return result;
            }

            /*
             * =================================================
             * 2. GET PURCHASE ORDER ITEMS SHEET
             * =================================================
             */
            Sheet itemsSheet =
                    workbook.getSheet(
                            ExcelConstants
                                    .PURCHASE_ORDER_ITEMS_SHEET);

            if (itemsSheet == null) {

                result.setValid(false);

                result.getErrors().add(
                        "Required sheet not found: "
                                + ExcelConstants
                                .PURCHASE_ORDER_ITEMS_SHEET);

                return result;
            }

            /*
             * =================================================
             * 3. VALIDATE MASTER SHEET
             * =================================================
             */
            validateSheet(
                    masterSheet,
                    ExcelConstants
                            .PURCHASE_ORDER_MASTER_SHEET,
                    result);

            /*
             * =================================================
             * 4. VALIDATE ITEMS SHEET
             * =================================================
             */
            validateSheet(
                    itemsSheet,
                    ExcelConstants
                            .PURCHASE_ORDER_ITEMS_SHEET,
                    result);

            /*
             * =================================================
             * 5. VALIDATE ITEMS AGAINST MASTER
             *
             * Mathematical validation:
             *
             * Rate + GST = Unit Value With GST
             *
             * Unit Value With GST * Qty = Net Amount
             *
             * SUM(Item Net Amount)
             *      =
             * Master Total Order Value
             *
             * =================================================
             */
            validatePurchaseOrderItemsAgainstMaster(
                    masterSheet,
                    itemsSheet,
                    result,
                    formulaEvaluator);

            /*
             * =================================================
             * 6. FINAL RESULT
             * =================================================
             */
            result.setValid(
                    result.getErrors().isEmpty()
                            && result.getRowErrors().isEmpty());

        } catch (Exception e) {

            result.setValid(false);

            result.getErrors().add(
                    "Unable to validate file: "
                            + e.getMessage());
        }

        return result;
    }

    /**
     * =====================================================
     * VALIDATE ONE SHEET
     * =====================================================
     */
    private void validateSheet(
            Sheet sheet,
            String sheetName,
            FileValidationResult result) {

        /*
         * =================================================
         * 1. FIND HEADER ROW
         * =================================================
         */
        int headerRowIndex =
                findHeaderRow(
                        sheet,
                        sheetName);

        if (headerRowIndex == -1) {

            result.getErrors().add(
                    "Header row not found in sheet: "
                            + sheetName);

            return;
        }

        /*
         * =================================================
         * 2. CREATE HEADER MAP
         * =================================================
         */
        Row headerRow =
                sheet.getRow(headerRowIndex);

        Map<String, Integer> headerMap =
                createHeaderMap(headerRow);

        /*
         * =================================================
         * 3. VALIDATE HEADERS
         * =================================================
         */
        if (!validateHeaders(
                headerMap,
                sheetName,
                result)) {

            return;
        }

        /*
         * =================================================
         * 4. VALIDATE DATA ROWS
         * =================================================
         */
        int totalRows = 0;

        /*
         * Purchase Order Number must be unique
         * only in Purchase Order Master.
         *
         * Items can have duplicate Order Numbers.
         */
        Set<String> orderNumbers =
                new HashSet<>();

        for (int rowIndex =
                     headerRowIndex + 1;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row =
                    sheet.getRow(rowIndex);

            if (row == null
                    || isEmptyRow(row)) {

                continue;
            }

            totalRows++;

            int excelRowNumber =
                    rowIndex + 1;

            /*
             * =================================================
             * PURCHASE ORDER NUMBER UNIQUE CHECK
             *
             * Only for Purchase Order Master
             * =================================================
             */
            if (ExcelConstants
                    .PURCHASE_ORDER_MASTER_SHEET
                    .equals(sheetName)) {

                Integer orderNoColumn =
                        headerMap.get("orderno");

                if (orderNoColumn != null) {

                    String orderNo =
                            getCellValue(
                                    row,
                                    orderNoColumn);

                    if (!orderNo.isEmpty()
                            && !orderNumbers.add(orderNo)) {

                        RowValidationError duplicateError =
                                new RowValidationError(
                                        excelRowNumber);

                        duplicateError.getErrors().add(
                                "[" + sheetName + "] "
                                        + "Duplicate Purchase Order Number: "
                                        + orderNo);

                        result.getRowErrors().add(
                                duplicateError);
                    }
                }
            }

            /*
             * =================================================
             * NORMAL ROW VALIDATION
             * =================================================
             */
            RowValidationError rowError =
                    purchaseOrderRowValidator.validate(
                            row,
                            excelRowNumber,
                            headerMap);

            if (!rowError.getErrors().isEmpty()) {

                RowValidationError sheetRowError =
                        new RowValidationError(
                                excelRowNumber);

                for (String error :
                        rowError.getErrors()) {

                    sheetRowError.getErrors().add(
                            "[" + sheetName + "] "
                                    + error);
                }

                result.getRowErrors().add(
                        sheetRowError);
            }
        }

        /*
         * =================================================
         * 5. SET RESULT INFORMATION
         * =================================================
         */
        result.setHeaderRow(
                headerRowIndex + 1);

        result.setHeaderStartRow(
                headerRowIndex + 1);

        result.setHeaderEndRow(
                headerRowIndex + 1);

        result.setDataStartRow(
                headerRowIndex + 2);

        result.setColumnCount(
                headerMap.size());

        /*
         * Add rows from both sheets.
         */
        result.setTotalRows(
                result.getTotalRows()
                        + totalRows);
    }

    /**
     * =====================================================
     * PURCHASE ORDER ITEMS VS MASTER VALIDATION
     *
     * Validations:
     *
     * 1. OrderNo must exist in Master
     *
     * 2. Unit Value With GST:
     *
     *    Rate + (Rate * GST / 100)
     *
     * 3. Net Amount:
     *
     *    Unit Value With GST * Qty
     *
     * 4. Grand Total:
     *
     *    SUM(Net Amount for same OrderNo)
     *
     * 5. Grand Total must match:
     *
     *    Master Total Order Value
     *
     * =====================================================
     */
    private void validatePurchaseOrderItemsAgainstMaster(
            Sheet masterSheet,
            Sheet itemsSheet,
            FileValidationResult result,
            FormulaEvaluator formulaEvaluator) {

        DataFormatter formatter =
                new DataFormatter();

        /*
         * =================================================
         * FIND MASTER HEADER
         * =================================================
         */
        int masterHeaderRowIndex =
                findHeaderRow(
                        masterSheet,
                        ExcelConstants
                                .PURCHASE_ORDER_MASTER_SHEET);

        /*
         * =================================================
         * FIND ITEMS HEADER
         * =================================================
         */
        int itemsHeaderRowIndex =
                findHeaderRow(
                        itemsSheet,
                        ExcelConstants
                                .PURCHASE_ORDER_ITEMS_SHEET);

        if (masterHeaderRowIndex == -1
                || itemsHeaderRowIndex == -1) {

            return;
        }

        /*
         * =================================================
         * CREATE HEADER MAPS
         * =================================================
         */
        Map<String, Integer> masterHeaderMap =
                createHeaderMap(
                        masterSheet.getRow(
                                masterHeaderRowIndex));

        Map<String, Integer> itemsHeaderMap =
                createHeaderMap(
                        itemsSheet.getRow(
                                itemsHeaderRowIndex));

        /*
         * =================================================
         * MASTER ORDER TOTALS
         *
         * orderNo -> Total Order Value
         * =================================================
         */
        Map<String, BigDecimal> masterOrderTotals =
                new HashMap<>();

        /*
         * orderNo -> Excel row number
         */
        Map<String, Integer> masterOrderRows =
                new HashMap<>();

        /*
         * =================================================
         * MASTER COLUMNS
         * =================================================
         */
        Integer masterOrderNoColumn =
                masterHeaderMap.get("orderno");

        Integer totalOrderValueColumn =
                masterHeaderMap.get("totalordervalue");

        if (masterOrderNoColumn == null) {

            result.getErrors().add(
                    "Required column missing in Purchase Order Master: orderno");

            return;
        }

        if (totalOrderValueColumn == null) {

            result.getErrors().add(
                    "Required column missing in Purchase Order Master: totalordervalue");

            return;
        }

        /*
         * =================================================
         * READ MASTER DATA
         * =================================================
         */
        for (int rowIndex =
                     masterHeaderRowIndex + 1;
             rowIndex <= masterSheet.getLastRowNum();
             rowIndex++) {

            Row row =
                    masterSheet.getRow(rowIndex);

            if (row == null
                    || isEmptyRow(row)) {

                continue;
            }

            String orderNo =
                    getCellValue(
                            row,
                            masterOrderNoColumn,
                            formatter,
                            formulaEvaluator);

            if (orderNo.isEmpty()) {
                continue;
            }

            BigDecimal totalOrderValue =
                    getBigDecimalCellValue(
                            row,
                            totalOrderValueColumn,
                            formatter,
                            formulaEvaluator);

            masterOrderTotals.put(
                    orderNo,
                    totalOrderValue);

            masterOrderRows.put(
                    orderNo,
                    rowIndex + 1);
        }

        /*
         * =================================================
         * ITEM COLUMNS
         * =================================================
         */
        Integer itemOrderNoColumn =
                itemsHeaderMap.get("orderno");

        Integer rateColumn =
                itemsHeaderMap.get("rate");

        Integer gstColumn =
                itemsHeaderMap.get("gst");

        Integer unitValueWithGstColumn =
                itemsHeaderMap.get("unitvaluewithgst");

        Integer qtyColumn =
                itemsHeaderMap.get("qty");

        Integer netAmountColumn =
                itemsHeaderMap.get("netamount");

        if (itemOrderNoColumn == null
                || rateColumn == null
                || gstColumn == null
                || unitValueWithGstColumn == null
                || qtyColumn == null
                || netAmountColumn == null) {

            result.getErrors().add(
                    "Required columns missing in Purchase Order Items "
                            + "for mathematical validation");

            return;
        }

        /*
         * =================================================
         * GRAND TOTAL PER ORDER
         *
         * orderNo -> SUM(Net Amount)
         * =================================================
         */
        Map<String, BigDecimal> itemGrandTotals =
                new HashMap<>();

        /*
         * =================================================
         * VALIDATE EVERY ITEM
         * =================================================
         */
        for (int rowIndex =
                     itemsHeaderRowIndex + 1;
             rowIndex <= itemsSheet.getLastRowNum();
             rowIndex++) {

            Row row =
                    itemsSheet.getRow(rowIndex);

            if (row == null
                    || isEmptyRow(row)) {

                continue;
            }

            int excelRowNumber =
                    rowIndex + 1;

            /*
             * =================================================
             * READ ORDER NUMBER
             * =================================================
             */
            String orderNo =
                    getCellValue(
                            row,
                            itemOrderNoColumn,
                            formatter,
                            formulaEvaluator);

            if (orderNo.isEmpty()) {
                continue;
            }

            /*
             * =================================================
             * 1. CHECK ORDER NUMBER EXISTS IN MASTER
             * =================================================
             */
            if (!masterOrderTotals.containsKey(orderNo)) {

                addRowError(
                        result,
                        excelRowNumber,
                        "[" + ExcelConstants
                                .PURCHASE_ORDER_ITEMS_SHEET + "] "
                                + "Purchase Order Number not found "
                                + "in Purchase Order Master: "
                                + orderNo);

                continue;
            }

            /*
             * =================================================
             * READ ITEM VALUES
             * =================================================
             */
            BigDecimal rate =
                    getBigDecimalCellValue(
                            row,
                            rateColumn,
                            formatter,
                            formulaEvaluator);

            BigDecimal gst =
                    getBigDecimalCellValue(
                            row,
                            gstColumn,
                            formatter,
                            formulaEvaluator);

            BigDecimal enteredUnitValueWithGst =
                    getBigDecimalCellValue(
                            row,
                            unitValueWithGstColumn,
                            formatter,
                            formulaEvaluator);

            BigDecimal qty =
                    getBigDecimalCellValue(
                            row,
                            qtyColumn,
                            formatter,
                            formulaEvaluator);

            BigDecimal enteredNetAmount =
                    getBigDecimalCellValue(
                            row,
                            netAmountColumn,
                            formatter,
                            formulaEvaluator);

            /*
             * =================================================
             * 2. CALCULATE GST AMOUNT
             *
             * GST Amount =
             * Rate * GST / 100
             * =================================================
             */
            BigDecimal gstAmount =
                    rate.multiply(gst)
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP);

            /*
             * =================================================
             * 3. CALCULATE UNIT VALUE WITH GST
             *
             * Rate + GST Amount
             * =================================================
             */
            BigDecimal calculatedUnitValueWithGst =
                    rate.add(gstAmount)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP);

            /*
             * =================================================
             * 4. VALIDATE UNIT VALUE WITH GST
             * =================================================
             */
            if (enteredUnitValueWithGst.compareTo(
                    calculatedUnitValueWithGst) != 0) {

                addRowError(
                        result,
                        excelRowNumber,
                        "[" + ExcelConstants
                                .PURCHASE_ORDER_ITEMS_SHEET + "] "
                                + "Invalid Unit Value With GST "
                                + "for OrderNo: "
                                + orderNo
                                + ". Expected: "
                                + calculatedUnitValueWithGst
                                + ", Actual: "
                                + enteredUnitValueWithGst);
            }

            /*
             * =================================================
             * 5. CALCULATE NET AMOUNT
             *
             * Unit Value With GST * Qty
             * =================================================
             */
            BigDecimal calculatedNetAmount =
                    calculatedUnitValueWithGst
                            .multiply(qty)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP);

            /*
             * =================================================
             * 6. VALIDATE NET AMOUNT
             * =================================================
             */
            if (enteredNetAmount.compareTo(
                    calculatedNetAmount) != 0) {

                addRowError(
                        result,
                        excelRowNumber,
                        "[" + ExcelConstants
                                .PURCHASE_ORDER_ITEMS_SHEET + "] "
                                + "Invalid Net Amount "
                                + "for OrderNo: "
                                + orderNo
                                + ". Expected: "
                                + calculatedNetAmount
                                + ", Actual: "
                                + enteredNetAmount);
            }

            /*
             * =================================================
             * 7. ADD NET AMOUNT TO GRAND TOTAL
             *
             * Same OrderNo can appear multiple times.
             * =================================================
             */
            itemGrandTotals.merge(
                    orderNo,
                    calculatedNetAmount,
                    BigDecimal::add);
        }

        /*
         * =================================================
         * 8. VALIDATE GRAND TOTAL AGAINST MASTER
         * =================================================
         */
        for (Map.Entry<String, BigDecimal> entry :
                itemGrandTotals.entrySet()) {

            String orderNo =
                    entry.getKey();

            BigDecimal calculatedGrandTotal =
                    entry.getValue()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP);

            BigDecimal masterTotal =
                    masterOrderTotals.get(orderNo);

            if (masterTotal == null) {
                continue;
            }

            masterTotal =
                    masterTotal.setScale(
                            2,
                            RoundingMode.HALF_UP);

            /*
             * =================================================
             * GRAND TOTAL MISMATCH
             * =================================================
             */
            if (calculatedGrandTotal.compareTo(
                    masterTotal) != 0) {

                Integer masterRow =
                        masterOrderRows.get(orderNo);

                addRowError(
                        result,
                        masterRow != null
                                ? masterRow
                                : 0,
                        "[" + ExcelConstants
                                .PURCHASE_ORDER_MASTER_SHEET + "] "
                                + "Total Order Value mismatch "
                                + "for OrderNo: "
                                + orderNo
                                + ". Expected from Items: "
                                + calculatedGrandTotal
                                + ", Actual Master Total Order Value: "
                                + masterTotal);
            }
        }
    }

    /**
     * =====================================================
     * GET CELL VALUE
     * =====================================================
     */
    private String getCellValue(
            Row row,
            Integer columnIndex) {

        if (columnIndex == null
                || row == null
                || row.getCell(columnIndex) == null) {

            return "";
        }

        return row.getCell(columnIndex)
                .toString()
                .trim();
    }

    /**
     * =====================================================
     * GET CELL VALUE
     *
     * Formula-safe version
     * =====================================================
     */
    private String getCellValue(
            Row row,
            Integer columnIndex,
            DataFormatter formatter,
            FormulaEvaluator formulaEvaluator) {

        if (columnIndex == null
                || row == null
                || row.getCell(columnIndex) == null) {

            return "";
        }

        return formatter
                .formatCellValue(
                        row.getCell(columnIndex),
                        formulaEvaluator)
                .trim();
    }

    /**
     * =====================================================
     * GET BIG DECIMAL CELL VALUE
     * =====================================================
     */
    private BigDecimal getBigDecimalCellValue(
            Row row,
            Integer columnIndex,
            DataFormatter formatter,
            FormulaEvaluator formulaEvaluator) {

        String value =
                getCellValue(
                        row,
                        columnIndex,
                        formatter,
                        formulaEvaluator);

        if (value.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {

            return new BigDecimal(
                    value
                            .replace(",", "")
                            .replace("₹", "")
                            .trim());

        } catch (NumberFormatException e) {

            return BigDecimal.ZERO;
        }
    }

    /**
     * =====================================================
     * ADD ROW ERROR
     * =====================================================
     */
    private void addRowError(
            FileValidationResult result,
            int excelRowNumber,
            String message) {

        RowValidationError rowError =
                new RowValidationError(
                        excelRowNumber);

        rowError.getErrors().add(
                message);

        result.getRowErrors().add(
                rowError);
    }

    /**
     * =====================================================
     * REQUIRED ABSTRACT METHOD
     *
     * Returns Purchase Order Master sheet.
     * =====================================================
     */
    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        return workbook.getSheet(
                ExcelConstants
                        .PURCHASE_ORDER_MASTER_SHEET);
    }

    /**
     * =====================================================
     * FIND HEADER ROW
     *
     * FOR PURCHASE ORDER MASTER
     * =====================================================
     */
    @Override
    protected int findHeaderRow(
            Sheet sheet) {

        for (int i = 0;
             i <= sheet.getLastRowNum();
             i++) {

            Row row =
                    sheet.getRow(i);

            if (row == null) {
                continue;
            }

            Map<String, Integer> headers =
                    createHeaderMap(row);

            if (headers.containsKey("ulbname")
                    && headers.containsKey("orderno")
                    && headers.containsKey("orderdate")
                    && headers.containsKey("ordername")) {

                return i;
            }
        }

        return -1;
    }

    /**
     * =====================================================
     * FIND HEADER ROW FOR A SPECIFIC SHEET
     * =====================================================
     */
    private int findHeaderRow(
            Sheet sheet,
            String sheetName) {

        for (int i = 0;
             i <= sheet.getLastRowNum();
             i++) {

            Row row =
                    sheet.getRow(i);

            if (row == null) {
                continue;
            }

            Map<String, Integer> headers =
                    createHeaderMap(row);

            /*
             * ---------------------------------------------
             * Purchase Order Master
             * ---------------------------------------------
             */
            if (ExcelConstants
                    .PURCHASE_ORDER_MASTER_SHEET
                    .equals(sheetName)) {

                if (headers.containsKey("ulbname")
                        && headers.containsKey("orderno")
                        && headers.containsKey("orderdate")
                        && headers.containsKey("ordername")
                        && headers.containsKey("totalordervalue")) {

                    return i;
                }
            }

            /*
             * ---------------------------------------------
             * Purchase Order Items
             * ---------------------------------------------
             */
            if (ExcelConstants
                    .PURCHASE_ORDER_ITEMS_SHEET
                    .equals(sheetName)) {

                if (headers.containsKey("ulbname")
                        && headers.containsKey("orderno")
                        && headers.containsKey("itemdescription")) {

                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * =====================================================
     * REQUIRED ABSTRACT METHOD
     *
     * Validate Purchase Order Master headers.
     * =====================================================
     */
    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        String[] requiredHeaders = {

                "ulbname",
                "orderno",
                "orderdate",
                "ordername",
                "suppliername",
                "sourceoffund",
                "department",
                "totalordervalue"
        };

        boolean valid = true;

        for (String header :
                requiredHeaders) {

            if (!headerMap.containsKey(header)) {

                result.getErrors().add(
                        "Required column missing: "
                                + header);

                valid = false;
            }
        }

        return valid;
    }

    /**
     * =====================================================
     * VALIDATE HEADERS FOR SPECIFIC SHEET
     * =====================================================
     */
    private boolean validateHeaders(
            Map<String, Integer> headerMap,
            String sheetName,
            FileValidationResult result) {

        String[] requiredHeaders;

        /*
         * ---------------------------------------------
         * Purchase Order Master
         * ---------------------------------------------
         */
        if (ExcelConstants
                .PURCHASE_ORDER_MASTER_SHEET
                .equals(sheetName)) {

            requiredHeaders =
                    new String[] {

                            "ulbname",
                            "orderno",
                            "orderdate",
                            "ordername",
                            "suppliername",
                            "sourceoffund",
                            "department",
                            "totalordervalue"
                    };

        } else {

            /*
             * ---------------------------------------------
             * Purchase Order Items
             * ---------------------------------------------
             */
            requiredHeaders =
                    new String[] {

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
        }

        boolean valid = true;

        for (String header :
                requiredHeaders) {

            if (!headerMap.containsKey(header)) {

                result.getErrors().add(
                        "Required column missing in "
                                + sheetName
                                + ": "
                                + header);

                valid = false;
            }
        }

        return valid;
    }
}