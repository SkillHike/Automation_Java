package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ReportUtils {
    public static void generateHTMLReport(String filePath, List<String[]> reportData) throws IOException {
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
                    if (rowData[0].equals("Tolerance") && j > 0) { // Tolerance row check
                        htmlBuilder.append("<td>").append(cell).append("</td>");
                    } else {
                        htmlBuilder.append("<td>").append(cell).append("</td>");
                    }
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






    public static void generateExcelReport(String filePath, List<String[]> reportData) throws IOException {
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
            tradeIdExcelRow.createCell(cellNum).setCellValue(tradeIdRow[0]);
            dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[0]);
            dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[0]);
            differenceExcelRow.createCell(cellNum).setCellValue(differenceRow[0]);
            toleranceExcelRow.createCell(cellNum).setCellValue("Tolerance");

            for (int j = 1; j < differenceRow.length; j++) {
                if (!"matched".equals(differenceRow[j])) {
                    cellNum++;
                    tradeIdExcelRow.createCell(cellNum).setCellValue(tradeIdRow[j]);
                    dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[j]);
                    dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[j]);
                    differenceExcelRow.createCell(cellNum).setCellValue(differenceRow[j]);

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
                }
            }

            sheet.createRow(rowNum++); // Empty row
        }

        Files.createDirectories(Paths.get(filePath).getParent());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }

        workbook.close();

        System.out.println("Excel report generated successfully at: " + filePath);
        System.out.println("Execution ended for Excel report generation.");
    }

    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
