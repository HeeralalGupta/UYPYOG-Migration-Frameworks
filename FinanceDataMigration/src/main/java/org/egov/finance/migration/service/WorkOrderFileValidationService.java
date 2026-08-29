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
import org.egov.finance.migration.service.validator.WorkOrderRowValidator;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class WorkOrderFileValidationService
        extends AbstractFileValidationService {

    private final WorkOrderRowValidator workOrderRowValidator;

    public WorkOrderFileValidationService(
            WorkOrderRowValidator workOrderRowValidator) {

        super(workOrderRowValidator);

        this.workOrderRowValidator =
                workOrderRowValidator;
    }

    /**
     * =====================================================
     * MODULE CODE
     * =====================================================
     */
    @Override
    protected MigrationType getModuleCode() {

        return MigrationType.WORK_ORDER;
    }

    /**
     * =====================================================
     * MAIN VALIDATION
     *
     * Work Order contains two sheets:
     *
     * 1. Work Order Master
     * 2. Work Order Items
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
             * NEW:
             * Formula evaluator
             *
             * This allows validation of Excel formula cells too.
             * =================================================
             */
            FormulaEvaluator formulaEvaluator =
                    workbook.getCreationHelper()
                            .createFormulaEvaluator();

            /*
             * =================================================
             * 1. GET WORK ORDER MASTER SHEET
             * =================================================
             */
            Sheet masterSheet =
                    workbook.getSheet(
                            ExcelConstants
                                    .WORK_ORDER_MASTER_SHEET);

            if (masterSheet == null) {

                result.setValid(false);

                result.getErrors().add(
                        "Required sheet not found: "
                                + ExcelConstants
                                .WORK_ORDER_MASTER_SHEET);

                return result;
            }

            /*
             * =================================================
             * 2. GET WORK ORDER ITEMS SHEET
             * =================================================
             */
            Sheet itemsSheet =
                    workbook.getSheet(
                            ExcelConstants
                                    .WORK_ORDER_ITEMS_SHEET);

            if (itemsSheet == null) {

                result.setValid(false);

                result.getErrors().add(
                        "Required sheet not found: "
                                + ExcelConstants
                                .WORK_ORDER_ITEMS_SHEET);

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
                            .WORK_ORDER_MASTER_SHEET,
                    result);

            /*
             * =================================================
             * 4. VALIDATE ITEMS SHEET
             * =================================================
             */
            validateSheet(
                    itemsSheet,
                    ExcelConstants
                            .WORK_ORDER_ITEMS_SHEET,
                    result);

            /*
             * =================================================
             * NEW - 5. CROSS SHEET + MATHEMATICAL VALIDATION
             *
             * Validates:
             *
             * Unit Rate + GST
             *          =
             * Unit Value With GST
             *
             * Unit Value With GST * Quantity
             *          =
             * Amount
             *
             * SUM(Amount)
             *          =
             * Total Order Amt
             *
             * WorkOrderNo in Items
             *          =
             * WorkOrderNo in Master
             * =================================================
             */
            validateWorkOrderItemsAgainstMaster(
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
         * Work Order Number must be unique
         * only in Master.
         *
         * Items can contain multiple rows
         * with same Work Order Number.
         */
        Set<String> workOrderNumbers =
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
             * WORK ORDER NUMBER UNIQUE CHECK
             *
             * Only Master.
             * =================================================
             */
            if (ExcelConstants
                    .WORK_ORDER_MASTER_SHEET
                    .equals(sheetName)) {

                Integer workOrderNoColumn =
                        headerMap.get("workorderno");

                if (workOrderNoColumn != null) {

                    String workOrderNo =
                            getCellValue(
                                    row,
                                    workOrderNoColumn);

                    if (!workOrderNo.isEmpty()
                            && !workOrderNumbers.add(
                                    workOrderNo)) {

                        RowValidationError duplicateError =
                                new RowValidationError(
                                        excelRowNumber);

                        duplicateError.getErrors().add(
                                "[" + sheetName + "] "
                                        + "Duplicate Work Order Number: "
                                        + workOrderNo);

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
                    workOrderRowValidator.validate(
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
     * NEW
     *
     * WORK ORDER ITEMS VS MASTER VALIDATION
     *
     * =====================================================
     *
     * Rules:
     *
     * 1. Work Order No in Items must exist in Master.
     *
     * 2. Unit Value With GST must be:
     *
     *    Unit Rate + (Unit Rate * GST / 100)
     *
     * 3. Amount must be:
     *
     *    Unit Value With GST * Quantity
     *
     * 4. Sum of Amount for each Work Order No must equal:
     *
     *    Master Total Order Amt
     *
     * =====================================================
     */
    private void validateWorkOrderItemsAgainstMaster(
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
                                .WORK_ORDER_MASTER_SHEET);

        /*
         * =================================================
         * FIND ITEMS HEADER
         * =================================================
         */
        int itemsHeaderRowIndex =
                findHeaderRow(
                        itemsSheet,
                        ExcelConstants
                                .WORK_ORDER_ITEMS_SHEET);

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
         * WorkOrderNo -> Total Order Amt
         * =================================================
         */
        Map<String, BigDecimal> masterOrderTotals =
                new HashMap<>();

        /*
         * WorkOrderNo -> Excel row number
         */
        Map<String, Integer> masterOrderRows =
                new HashMap<>();

        /*
         * =================================================
         * MASTER COLUMNS
         * =================================================
         */
        Integer masterWorkOrderNoColumn =
                masterHeaderMap.get("workorderno");

        /*
         * Excel:
         *
         * Total Order Amt *
         *
         * should normalize to:
         *
         * totalorderamt
         * =================================================
         */
        Integer totalOrderAmtColumn =
                masterHeaderMap.get("totalorderamt");

        if (masterWorkOrderNoColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Master: "
                            + "workorderno");

            return;
        }

        if (totalOrderAmtColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Master: "
                            + "totalorderamt");

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

            String workOrderNo =
                    getCellValue(
                            row,
                            masterWorkOrderNoColumn,
                            formatter,
                            formulaEvaluator);

            if (workOrderNo.isEmpty()) {
                continue;
            }

            BigDecimal totalOrderAmt =
                    getBigDecimalCellValue(
                            row,
                            totalOrderAmtColumn,
                            formatter,
                            formulaEvaluator);

            masterOrderTotals.put(
                    workOrderNo,
                    totalOrderAmt);

            masterOrderRows.put(
                    workOrderNo,
                    rowIndex + 1);
        }

        /*
         * =================================================
         * ITEMS COLUMNS
         * =================================================
         */
        Integer itemWorkOrderNoColumn =
                itemsHeaderMap.get("workorderno");

        Integer unitRateColumn =
                itemsHeaderMap.get("unitrate");

        Integer gstColumn =
                itemsHeaderMap.get("gst");

        Integer unitValueWithGstColumn =
                itemsHeaderMap.get(
                        "unitvaluewithgst");

        Integer quantityColumn =
                itemsHeaderMap.get("quantity");

        Integer amountColumn =
                itemsHeaderMap.get("amount");

        if (itemWorkOrderNoColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Items: "
                            + "workorderno");

            return;
        }

        if (unitRateColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Items: "
                            + "unitrate");

            return;
        }

        if (gstColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Items: "
                            + "gst");

            return;
        }

        if (unitValueWithGstColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Items: "
                            + "unitvaluewithgst");

            return;
        }

        if (quantityColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Items: "
                            + "quantity");

            return;
        }

        if (amountColumn == null) {

            result.getErrors().add(
                    "Required column missing in Work Order Items: "
                            + "amount");

            return;
        }

        /*
         * =================================================
         * GRAND TOTAL PER WORK ORDER
         *
         * WorkOrderNo -> SUM(Amount)
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
             * GET WORK ORDER NUMBER
             * =================================================
             */
            String workOrderNo =
                    getCellValue(
                            row,
                            itemWorkOrderNoColumn,
                            formatter,
                            formulaEvaluator);

            if (workOrderNo.isEmpty()) {
                continue;
            }

            /*
             * =================================================
             * NEW:
             *
             * CHECK WORK ORDER NUMBER EXISTS IN MASTER
             * =================================================
             */
            if (!masterOrderTotals.containsKey(
                    workOrderNo)) {

                addRowError(
                        result,
                        excelRowNumber,
                        "[" + ExcelConstants
                                .WORK_ORDER_ITEMS_SHEET
                                + "] "
                                + "Work Order Number not found "
                                + "in Work Order Master: "
                                + workOrderNo);

                continue;
            }

            /*
             * =================================================
             * READ ITEM VALUES
             * =================================================
             */
            BigDecimal unitRate =
                    getBigDecimalCellValue(
                            row,
                            unitRateColumn,
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

            BigDecimal quantity =
                    getBigDecimalCellValue(
                            row,
                            quantityColumn,
                            formatter,
                            formulaEvaluator);

            BigDecimal enteredAmount =
                    getBigDecimalCellValue(
                            row,
                            amountColumn,
                            formatter,
                            formulaEvaluator);

            /*
             * =================================================
             * NEW:
             *
             * CALCULATE GST AMOUNT
             *
             * Unit Rate * GST / 100
             * =================================================
             */
            BigDecimal gstAmount =
                    unitRate
                            .multiply(gst)
                            .divide(
                                    BigDecimal.valueOf(100),
                                    2,
                                    RoundingMode.HALF_UP);

            /*
             * =================================================
             * NEW:
             *
             * CALCULATE UNIT VALUE WITH GST
             *
             * Unit Rate + GST Amount
             * =================================================
             */
            BigDecimal calculatedUnitValueWithGst =
                    unitRate
                            .add(gstAmount)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP);

            /*
             * =================================================
             * NEW:
             *
             * VALIDATE UNIT VALUE WITH GST
             * =================================================
             */
            if (enteredUnitValueWithGst.compareTo(
                    calculatedUnitValueWithGst) != 0) {

                addRowError(
                        result,
                        excelRowNumber,
                        "[" + ExcelConstants
                                .WORK_ORDER_ITEMS_SHEET
                                + "] "
                                + "Invalid Unit Value With GST "
                                + "for Work Order No: "
                                + workOrderNo
                                + ". Expected: "
                                + calculatedUnitValueWithGst
                                + ", Actual: "
                                + enteredUnitValueWithGst);
            }

            /*
             * =================================================
             * NEW:
             *
             * CALCULATE AMOUNT
             *
             * Unit Value With GST * Quantity
             * =================================================
             */
            BigDecimal calculatedAmount =
                    calculatedUnitValueWithGst
                            .multiply(quantity)
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP);

            /*
             * =================================================
             * NEW:
             *
             * VALIDATE AMOUNT
             * =================================================
             */
            if (enteredAmount.compareTo(
                    calculatedAmount) != 0) {

                addRowError(
                        result,
                        excelRowNumber,
                        "[" + ExcelConstants
                                .WORK_ORDER_ITEMS_SHEET
                                + "] "
                                + "Invalid Amount "
                                + "for Work Order No: "
                                + workOrderNo
                                + ". Expected: "
                                + calculatedAmount
                                + ", Actual: "
                                + enteredAmount);
            }

            /*
             * =================================================
             * NEW:
             *
             * ADD AMOUNT TO GRAND TOTAL
             *
             * Multiple item rows can have the same
             * Work Order Number.
             * =================================================
             */
            itemGrandTotals.merge(
                    workOrderNo,
                    calculatedAmount,
                    BigDecimal::add);
        }

        /*
         * =================================================
         * NEW:
         *
         * COMPARE GRAND TOTAL WITH MASTER TOTAL
         * =================================================
         */
        for (Map.Entry<String, BigDecimal> entry :
                itemGrandTotals.entrySet()) {

            String workOrderNo =
                    entry.getKey();

            BigDecimal calculatedGrandTotal =
                    entry.getValue()
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP);

            BigDecimal masterTotal =
                    masterOrderTotals.get(
                            workOrderNo);

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
                        masterOrderRows.get(
                                workOrderNo);

                addRowError(
                        result,
                        masterRow != null
                                ? masterRow
                                : 0,
                        "[" + ExcelConstants
                                .WORK_ORDER_MASTER_SHEET
                                + "] "
                                + "Total Order Amt mismatch "
                                + "for Work Order No: "
                                + workOrderNo
                                + ". Expected from Items: "
                                + calculatedGrandTotal
                                + ", Actual Master Total Order Amt: "
                                + masterTotal);
            }
        }

        /*
         * =================================================
         * NEW:
         *
         * CHECK MASTER ORDERS WHICH HAVE NO ITEMS
         *
         * Example:
         *
         * Master:
         * WO001 = 100000
         *
         * Items:
         * No WO001
         *
         * This should fail.
         * =================================================
         */
        for (Map.Entry<String, BigDecimal> entry :
                masterOrderTotals.entrySet()) {

            String workOrderNo =
                    entry.getKey();

            BigDecimal masterTotal =
                    entry.getValue();

            if (!itemGrandTotals.containsKey(
                    workOrderNo)) {

                /*
                 * If master total is not zero,
                 * items are missing.
                 */
                if (masterTotal.compareTo(
                        BigDecimal.ZERO) != 0) {

                    Integer masterRow =
                            masterOrderRows.get(
                                    workOrderNo);

                    addRowError(
                            result,
                            masterRow != null
                                    ? masterRow
                                    : 0,
                            "[" + ExcelConstants
                                    .WORK_ORDER_MASTER_SHEET
                                    + "] "
                                    + "No items found for "
                                    + "Work Order No: "
                                    + workOrderNo
                                    + ". Master Total Order Amt: "
                                    + masterTotal);
                }
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
     * Returns Work Order Master sheet.
     * =====================================================
     */
    @Override
    protected Sheet getSheet(
            Workbook workbook) {

        return workbook.getSheet(
                ExcelConstants
                        .WORK_ORDER_MASTER_SHEET);
    }

    /**
     * =====================================================
     * REQUIRED ABSTRACT METHOD
     *
     * Find Master header row.
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

            /*
             * CHANGED:
             *
             * totalorderamt added to header detection.
             */
            if (headers.containsKey("ulbname")
                    && headers.containsKey("tendernumber")
                    && headers.containsKey("workorderno")
                    && headers.containsKey("workorderdate")
                    && headers.containsKey("totalorderamt")) {

                return i;
            }
        }

        return -1;
    }

    /**
     * =====================================================
     * FIND HEADER ROW FOR SPECIFIC SHEET
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
             * Work Order Master
             * ---------------------------------------------
             */
            if (ExcelConstants
                    .WORK_ORDER_MASTER_SHEET
                    .equals(sheetName)) {

                /*
                 * CHANGED:
                 *
                 * totalorderamt added.
                 */
                if (headers.containsKey("ulbname")
                        && headers.containsKey("tendernumber")
                        && headers.containsKey("workorderno")
                        && headers.containsKey("workorderdate")
                        && headers.containsKey("totalorderamt")) {

                    return i;
                }
            }

            /*
             * ---------------------------------------------
             * Work Order Items
             * ---------------------------------------------
             */
            if (ExcelConstants
                    .WORK_ORDER_ITEMS_SHEET
                    .equals(sheetName)) {

                if (headers.containsKey("tendernumber")
                        && headers.containsKey("workorderno")
                        && headers.containsKey("itemname")) {

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
     * Validate Work Order Master headers.
     * =====================================================
     */
    @Override
    protected boolean validateHeaders(
            Map<String, Integer> headerMap,
            FileValidationResult result) {

        /*
         * CHANGED:
         *
         * totalorderamt added.
         */
        String[] requiredHeaders = {

                "ulbname",
                "tendernumber",
                "workorderno",
                "workorderdate",
                "workordername",
                "workordertype",
                "active",
                "contractorname",
                "workname",
                "workcode",
                "totalorderamt",
                "fund",
                "department",
                "scheme"
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
         * Work Order Master
         * ---------------------------------------------
         */
        if (ExcelConstants
                .WORK_ORDER_MASTER_SHEET
                .equals(sheetName)) {

            /*
             * CHANGED:
             *
             * totalorderamt added.
             */
            requiredHeaders =
                    new String[] {

                            "ulbname",
                            "tendernumber",
                            "workorderno",
                            "workorderdate",
                            "workordername",
                            "workordertype",
                            "active",
                            "contractorname",
                            "workname",
                            "workcode",
                            "totalorderamt",
                            "fund",
                            "department",
                            "scheme"
                    };

        } else {

            /*
             * ---------------------------------------------
             * Work Order Items
             * ---------------------------------------------
             *
             * CHANGED:
             *
             * Added all fields required for mathematical
             * validation.
             */
            requiredHeaders =
                    new String[] {

                            "tendernumber",
                            "workorderno",
                            "itemname",
                            "unit",
                            "unitrate",
                            "gst",
                            "unitvaluewithgst",
                            "quantity",
                            "amount"
                    };
        }

        boolean valid = true;

        for (String header :
                requiredHeaders) {

            if (!headerMap.containsKey(
                    header)) {

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