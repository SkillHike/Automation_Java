package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

public class ReportUtils {

    public static void generateHTMLReport(String filePath, List<String[]> reportData, Set<String> primaryKeys) throws IOException {
        if (reportData.isEmpty()) {
            System.out.println("No data to generate report. HTML report will not be generated.");
            return;
        }


        System.out.println("Execution started for HTML report generation.");

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><head><title>Comparison Report</title></head><body>");
        htmlBuilder.append("<h1>Comparison Report</h1>");
        htmlBuilder.append("<table border='1'>");

        // Initialize a boolean array to track columns to remove
        boolean[] removeColumn = new boolean[reportData.get(0).length];

        // Check for columns with all empty or matched cells (excluding header)
        for (int j = 0; j < reportData.get(0).length; j++) {
            boolean isEmptyColumn = true;
            boolean isMatchedColumn = true;
            for (int i = 0; i < reportData.size(); i++) {
                String[] rowData = reportData.get(i);
                if (!"".equals(rowData[j].trim())) {
                    isEmptyColumn = false;
                }
                if (!"matched".equals(rowData[j])) {
                    isMatchedColumn = false;
                }
            }
            removeColumn[j] = isEmptyColumn || isMatchedColumn;
        }

        // Iterate over the reportData in chunks of 4 (column names, dataInEnv1, dataInEnv2, difference)
        for (int i = 0; i < reportData.size(); i += 4) {
            if (i + 3 >= reportData.size()) {
                System.out.println("Skipping incomplete set of rows at index: " + i);
                break; // Skip incomplete sets of rows
            }

            String[] columnNames = reportData.get(i);
            String[] dataInEnv1 = reportData.get(i + 1);
            String[] dataInEnv2 = reportData.get(i + 2);
            String[] difference = reportData.get(i + 3);

            // Check if there's an actual difference
            boolean hasDifference = false;
            for (int j = 1; j < difference.length; j++) {
                if (!"matched".equals(difference[j])) {
                    hasDifference = true;
                    break;
                }
            }

            // Skip processing if there are no differences
            if (!hasDifference) {
                continue;
            }

            // Begin adding rows to the HTML table
            htmlBuilder.append("<tr>");

            // First column header as "Column Names"
            htmlBuilder.append("<th>").append("Column Names").append("</th>");

            // Iterate over column names to create header row
            for (int j = 0; j < columnNames.length; j++) {
                if (!removeColumn[j]) {
                    boolean isPrimaryKey = primaryKeys.contains(columnNames[j]);
                    if (isPrimaryKey) {
                        htmlBuilder.append("<th style='background-color: orange;'>").append(columnNames[j]).append("</th>");
                    } else {
                        htmlBuilder.append("<th>").append(columnNames[j]).append("</th>");
                    }
                }
            }
            htmlBuilder.append("</tr>");


            // Data in Env1 row
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append("Data in Env1").append("</td>");
            for (int j = 0; j < dataInEnv1.length; j++) {
                if (!removeColumn[j]) {
                    htmlBuilder.append("<td>").append(dataInEnv1[j]).append("</td>");
                }
            }
            htmlBuilder.append("</tr>");

            // Data in Env2 row
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append("Data in Env2").append("</td>");
            for (int j = 0; j < dataInEnv2.length; j++) {
                if (!removeColumn[j]) {
                    htmlBuilder.append("<td>").append(dataInEnv2[j]).append("</td>");
                }
            }
            htmlBuilder.append("</tr>");

            // Difference row
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append("Difference").append("</td>");
            for (int j = 0; j < difference.length; j++) {
                if (!removeColumn[j]) {
                    String cell = difference[j];
                    if (!"matched".equals(cell) && !"".equals(cell.trim())) {
                        double differenceValue = parseDouble(cell);
                        String cellStyle = getToleranceCellStyle(differenceValue);
                        htmlBuilder.append("<td style='").append(cellStyle).append("'>").append(cell).append("</td>");
                    } else {
                        htmlBuilder.append("<td></td>"); // Skip matched or blank cell
                    }
                }
            }
            htmlBuilder.append("</tr>");

            // Insert Tolerance row after Difference row
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append("Tolerance").append("</td>");
            for (int j = 0; j < difference.length; j++) {
                if (!removeColumn[j]) {
                    String cell = difference[j];
                    if (!"matched".equals(cell) && !"".equals(cell.trim())) {
                        double differenceValue = parseDouble(cell);
                        String cellStyle = getToleranceCellStyle(differenceValue);
                        htmlBuilder.append("<td style='").append(cellStyle).append("'>").append(getToleranceLabel(differenceValue)).append("</td>");
                    } else {
                        htmlBuilder.append("<td></td>"); // Skip matched or blank cell
                    }
                }
            }
            htmlBuilder.append("</tr>");

            // No Primary Key row included

            htmlBuilder.append("<tr><td colspan='").append(columnNames.length + 1).append("'></td></tr>"); // Empty row after each set of rows
        }

        htmlBuilder.append("</table></body></html>");

        // Write the HTML content to file
        Files.createDirectories(Paths.get(filePath).getParent());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(htmlBuilder.toString().getBytes());
        }

        System.out.println("HTML report generated successfully at: " + filePath);
        System.out.println("Execution ended for HTML report generation.");
    }


    private static String getToleranceCellStyle(double differenceValue) {
        if (!Double.isNaN(differenceValue)) {
            if (Math.abs(differenceValue) < 0.5) {
                return "background-color:green;";
            } else if (Math.abs(differenceValue) < 1) {
                return "background-color:yellow;";
            } else {
                return "background-color:red;";
            }
        }
        return "";
    }




    private static String getToleranceLabel(double differenceValue) {
        if (!Double.isNaN(differenceValue)) {
            if (Math.abs(differenceValue) < 0.5) {
                return "Low";
            } else if (Math.abs(differenceValue) < 1) {
                return "Medium";
            } else {
                return "High";
            }
        }
        return "";
    }

    public static void generateExcelReport(String filePath, List<String[]> reportData, Set<String> primaryKeys) throws IOException {
        if (reportData.isEmpty()) {
            System.out.println("No data to generate report. Excel report will not be generated.");
            return;
        }

        System.out.println("Execution started for Excel report generation.");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Comparison Results");

        // Create reusable cell styles
        CellStyle greenStyle = workbook.createCellStyle();
        greenStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
        greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle yellowStyle = workbook.createCellStyle();
        yellowStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle redStyle = workbook.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        int rowNum = 0;

        for (int i = 0; i < reportData.size(); i += 4) {
            if (i + 3 >= reportData.size()) {
                System.out.println("Skipping incomplete set of rows at index: " + i);
                break; // Skip incomplete sets of rows
            }

            String[] tradeIdRow = reportData.get(i);
            String[] dataInEnv1 = reportData.get(i + 1);
            String[] dataInEnv2 = reportData.get(i + 2);
            String[] differenceRow = reportData.get(i + 3);

            // Check if there's an actual difference
            boolean hasDifference = false;
            for (int j = 1; j < differenceRow.length; j++) {
                if (!"matched".equals(differenceRow[j])) {
                    hasDifference = true;
                    break;
                }
            }

            if (!hasDifference) {
                continue; // Skip rows with no differences
            }

            // Create rows for unmatched columns only
            Row tradeIdExcelRow = sheet.createRow(rowNum++);
            Row dataInEnv1ExcelRow = sheet.createRow(rowNum++);
            Row dataInEnv2ExcelRow = sheet.createRow(rowNum++);
            Row differenceExcelRow = sheet.createRow(rowNum++);
            Row toleranceExcelRow = sheet.createRow(rowNum++);

            int cellNum = 0;
            Cell tradeIdCell = tradeIdExcelRow.createCell(cellNum);
            tradeIdCell.setCellValue("Column names");
            tradeIdCell.setCellStyle(boldStyle);

            dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[0]);
            dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[0]);
            differenceExcelRow.createCell(cellNum).setCellValue(differenceRow[0]);
            toleranceExcelRow.createCell(cellNum).setCellValue("Tolerance");

            for (int j = 1; j < differenceRow.length; j++) {
                if (!"matched".equals(differenceRow[j]) && !"".equals(differenceRow[j])) {
                    cellNum++;
                    Cell headerCell = tradeIdExcelRow.createCell(cellNum);
                    headerCell.setCellValue(tradeIdRow[j]);
                    headerCell.setCellStyle(boldStyle);

                    dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[j]);
                    dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[j]);
                    Cell diffCell = differenceExcelRow.createCell(cellNum);
                    diffCell.setCellValue(differenceRow[j]);

                    double differenceValue = parseDouble(differenceRow[j]);
                    if (!Double.isNaN(differenceValue)) {
                        if (Math.abs(differenceValue) < 0.5) {
                            diffCell.setCellStyle(greenStyle);
                        } else if (Math.abs(differenceValue) < 1) {
                            diffCell.setCellStyle(yellowStyle);
                        } else {
                            diffCell.setCellStyle(redStyle);
                        }
                    }

                    Cell toleranceCell = toleranceExcelRow.createCell(cellNum);
                    toleranceCell.setCellValue(getToleranceLabel(differenceValue));
                    if (!Double.isNaN(differenceValue)) {
                        if (Math.abs(differenceValue) < 0.5) {
                            toleranceCell.setCellStyle(greenStyle);
                        } else if (Math.abs(differenceValue) < 1) {
                            toleranceCell.setCellStyle(yellowStyle);
                        } else {
                            toleranceCell.setCellStyle(redStyle);
                        }
                    }
                }
            }

            sheet.createRow(rowNum++); // Empty row
        }

        Files.createDirectories(Paths.get(filePath).getParent());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }

        System.out.println("Excel report generated successfully at: " + filePath);
        System.out.println("Execution ended for Excel report generation.");
    }


    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
