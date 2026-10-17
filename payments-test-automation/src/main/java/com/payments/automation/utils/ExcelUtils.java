package com.payments.automation.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Excel (.xlsx / .xls) helper for data-driven testing.
 *
 * <p>Convention: row 1 = column headers. Add an {@code Execute} column with Y/N
 * to switch rows on or off without editing code.</p>
 */
public final class ExcelUtils {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelUtils() {
    }

    /** Reads every data row of a sheet as header -> value maps. */
    public static List<Map<String, String>> readSheet(Path file, String sheetName) {
        try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = requireSheet(wb, sheetName);
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            Row header = sheet.getRow(sheet.getFirstRowNum());
            List<String> headers = new ArrayList<>();
            for (Cell cell : header) {
                headers.add(FORMATTER.formatCellValue(cell).trim());
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlank(row)) {
                    continue;
                }
                Map<String, String> data = new LinkedHashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    data.put(headers.get(c), cell == null ? "" : FORMATTER.formatCellValue(cell, evaluator).trim());
                }
                rows.add(data);
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read Excel " + file, e);
        }
    }

    /** Only rows where Execute = Y (or rows without an Execute column). */
    public static List<Map<String, String>> readExecutableRows(Path file, String sheetName) {
        return filter(readSheet(file, sheetName), row -> !row.containsKey("Execute")
                || "Y".equalsIgnoreCase(row.get("Execute")));
    }

    /** Single row identified by a key column, e.g. TestCaseId = TC_001. */
    public static Map<String, String> readRow(Path file, String sheetName, String keyColumn, String keyValue) {
        return readSheet(file, sheetName).stream()
                .filter(row -> keyValue.equals(row.get(keyColumn)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(keyColumn + "=" + keyValue + " not found in " + sheetName));
    }

    public static List<Map<String, String>> filter(List<Map<String, String>> rows, Predicate<Map<String, String>> predicate) {
        return rows.stream().filter(predicate).toList();
    }

    /** Converts rows to Object[][] for a TestNG @DataProvider (each row passed as one Map argument). */
    public static Object[][] toDataProvider(List<Map<String, String>> rows) {
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    public static String getCellData(Path file, String sheetName, int rowIndex, int colIndex) {
        try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            Row row = requireSheet(wb, sheetName).getRow(rowIndex);
            return row == null ? "" : FORMATTER.formatCellValue(row.getCell(colIndex));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int getRowCount(Path file, String sheetName) {
        try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            return requireSheet(wb, sheetName).getLastRowNum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes a value into an existing workbook (e.g. Result / ActualStatus column). */
    public static void setCellData(Path file, String sheetName, int rowIndex, int colIndex, String value) {
        try {
            Workbook wb;
            try (InputStream in = Files.newInputStream(file)) {
                wb = WorkbookFactory.create(in);
            }
            try (wb) {
                Sheet sheet = requireSheet(wb, sheetName);
                Row row = sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
                row.createCell(colIndex).setCellValue(value);
                try (OutputStream out = Files.newOutputStream(file)) {
                    wb.write(out);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes the result into the column with the given header for the row whose keyColumn matches. */
    public static void writeResult(Path file, String sheetName, String keyColumn, String keyValue,
                                   String resultColumn, String result) {
        List<Map<String, String>> rows = readSheet(file, sheetName);
        List<String> headers = new ArrayList<>(rows.isEmpty() ? List.of() : rows.get(0).keySet());
        int keyIdx = headers.indexOf(keyColumn);
        int resIdx = headers.indexOf(resultColumn);
        if (keyIdx < 0 || resIdx < 0) {
            throw new IllegalArgumentException("Column not found: " + keyColumn + " / " + resultColumn);
        }
        for (int i = 0; i < rows.size(); i++) {
            if (keyValue.equals(rows.get(i).get(keyColumn))) {
                setCellData(file, sheetName, i + 1, resIdx, result);
                return;
            }
        }
    }

    /** Creates a new workbook from header + rows - handy for generating test data or exporting DB results. */
    public static void createWorkbook(Path file, String sheetName, List<String> headers, List<List<Object>> rows) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName);
            CellStyle bold = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            bold.setFont(font);
            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.size(); c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(bold);
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows.get(r).size(); c++) {
                    Object v = rows.get(r).get(c);
                    if (v instanceof Number n) {
                        row.createCell(c).setCellValue(n.doubleValue());
                    } else {
                        row.createCell(c).setCellValue(v == null ? "" : v.toString());
                    }
                }
            }
            for (int c = 0; c < headers.size(); c++) {
                sheet.autoSizeColumn(c);
            }
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                wb.write(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Sheet requireSheet(Workbook wb, String name) {
        Sheet sheet = wb.getSheet(name);
        if (sheet == null) {
            throw new IllegalArgumentException("Sheet not found: " + name);
        }
        return sheet;
    }

    private static boolean isBlank(Row row) {
        for (Cell cell : row) {
            if (!FORMATTER.formatCellValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
