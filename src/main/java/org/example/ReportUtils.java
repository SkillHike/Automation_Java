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

        for (int i = 0; i < reportData.size(); i++) {
            String[] rowData = reportData.get(i);
            htmlBuilder.append("<tr>");
            for (int j = 0; j < rowData.length; j++) {
                String cell = rowData[j];
                if (cell != null && !cell.isEmpty()) {
                    String cellStyle = "";
                    if (i % 4 == 0) {  // Bold the Trade ID row
                        cellStyle = "font-weight:bold;";
                    } else if (rowData[0].equals("Difference") && j > 0 && !"matched".equals(cell)) {
                        cellStyle = "background-color:yellow;";
                    }

                    htmlBuilder.append("<td style='" + cellStyle + "'>").append(cell).append("</td>");
                } else {
                    htmlBuilder.append("<td></td>");
                }
            }
            htmlBuilder.append("</tr>");

            if (rowData[0].equals("Difference")) {
                // Insert Tolerance row after Difference row
                String[] toleranceRow = new String[rowData.length];
                toleranceRow[0] = "Tolerance";
                for (int k = 1; k < rowData.length; k++) {
                    double difference = parseDouble(rowData[k]);
                    if (!Double.isNaN(difference) && Math.abs(difference) > 0.5) {
                        toleranceRow[k] = "Yes";
                    } else {
                        toleranceRow[k] = "No";
                    }
                }
                htmlBuilder.append("<tr>");
                for (int j = 0; j < toleranceRow.length; j++) {
                    String cell = toleranceRow[j];
                    if ("Yes".equals(cell)) {
                        htmlBuilder.append("<td style='background-color:red;'>").append(cell).append("</td>");
                    } else if ("No".equals(cell)) {
                        htmlBuilder.append("<td style='background-color:green;'>").append(cell).append("</td>");
                    } else {
                        htmlBuilder.append("<td>").append(cell).append("</td>");
                    }
                }
                htmlBuilder.append("</tr>");

                // Insert Primary Key row after Tolerance row
                String[] primaryKeyRow = new String[rowData.length];
                primaryKeyRow[0] = "Primary Key";
                for (int k = 1; k < rowData.length; k++) {

                    primaryKeyRow[k] = primaryKeys.contains(rowData[k]) ? "No" : "Yes";
                }
                htmlBuilder.append("<tr>");
                for (int j = 0; j < primaryKeyRow.length; j++) {
                    String cell = primaryKeyRow[j];
                    if ("Yes".equalsIgnoreCase(cell)) {
                        htmlBuilder.append("<td style='background-color:blue;'>").append(cell).append("</td>");
                    } else {
                        htmlBuilder.append("<td>").append(cell).append("</td>");
                    }
                }
                htmlBuilder.append("</tr>");
            }

            if (i % 5 == 4) {
                htmlBuilder.append("<tr><td colspan='").append(reportData.get(0).length).append("'></td></tr>");  // Empty row after each set of rows
            }
        }

        htmlBuilder.append("</table></body></html>");

        Files.createDirectories(Paths.get(filePath).getParent());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(htmlBuilder.toString().getBytes());
        }

        System.out.println("HTML report generated successfully at: " + filePath);
        System.out.println("Execution ended for HTML report generation.");
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

        CellStyle redStyle = workbook.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        CellStyle yellowStyle = workbook.createCellStyle();
        yellowStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle blueStyle = workbook.createCellStyle();
        blueStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        blueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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
            Row primaryKeyExcelRow = sheet.createRow(rowNum++);

            int cellNum = 0;
            Cell tradeIdCell = tradeIdExcelRow.createCell(cellNum);
            tradeIdCell.setCellValue(tradeIdRow[0]);
            tradeIdCell.setCellStyle(boldStyle);

            dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[0]);
            dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[0]);
            differenceExcelRow.createCell(cellNum).setCellValue(differenceRow[0]);
            toleranceExcelRow.createCell(cellNum).setCellValue("Tolerance");
            primaryKeyExcelRow.createCell(cellNum).setCellValue("Primary Key");

            for (int j = 1; j < differenceRow.length; j++) {
                if (!"matched".equals(differenceRow[j])) {
                    cellNum++;
                    Cell headerCell = tradeIdExcelRow.createCell(cellNum);
                    headerCell.setCellValue(tradeIdRow[j]);
                    headerCell.setCellStyle(boldStyle);

                    dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[j]);
                    dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[j]);
                    Cell diffCell = differenceExcelRow.createCell(cellNum);
                    diffCell.setCellValue(differenceRow[j]);
                    diffCell.setCellStyle(yellowStyle);

                    double differenceValue = parseDouble(differenceRow[j]);
                    Cell toleranceCell = toleranceExcelRow.createCell(cellNum);
                    if (!Double.isNaN(differenceValue)) {
                        if (Math.abs(differenceValue) > 0.5) {
                            toleranceCell.setCellValue("Yes");
                            toleranceCell.setCellStyle(redStyle);
                        } else {
                            toleranceCell.setCellValue("No");
                            toleranceCell.setCellStyle(greenStyle);
                        }
                    }

                    // Check if the column is a primary key
                    Cell primaryKeyCell = primaryKeyExcelRow.createCell(cellNum);
                    primaryKeyCell.setCellValue(primaryKeys.contains(tradeIdRow[j]) ? "Yes" : "No");
                    primaryKeyCell.setCellStyle(blueStyle);
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