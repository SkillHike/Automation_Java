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

        for (String[] rowData : reportData) {
            htmlBuilder.append("<tr>");
            for (int i = 0; i < rowData.length; i++) {
                String cell = rowData[i];
                if (cell != null && !cell.isEmpty()) {
                    if (i > 0 && "Difference".equals(rowData[0])) {
                        if ("matched".equals(cell)) {
                            htmlBuilder.append("<td style='background-color:green;'>").append(cell).append("</td>");
                        } else {
                            htmlBuilder.append("<td style='background-color:red;'>").append(cell).append("</td>");
                        }
                    } else {
                        htmlBuilder.append("<td>").append(cell).append("</td>");
                    }
                } else {
                    htmlBuilder.append("<td></td>");
                }
            }
            htmlBuilder.append("</tr>");
            htmlBuilder.append("<tr><td colspan='").append(reportData.get(0).length).append("'></td></tr>");  // Empty row
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
        int rowCount = 0;

        for (String[] rowData : reportData) {
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < rowData.length; i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData[i]);
                if (i > 0 && "Difference".equals(rowData[0])) {
                    if ("matched".equals(rowData[i])) {
                        CellStyle greenStyle = workbook.createCellStyle();
                        greenStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                        greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        cell.setCellStyle(greenStyle);
                    } else {
                        CellStyle redStyle = workbook.createCellStyle();
                        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
                        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                        cell.setCellStyle(redStyle);
                    }
                }
            }
            sheet.createRow(rowCount++);  // Empty row
        }

        Files.createDirectories(Paths.get(filePath).getParent());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }
        workbook.close();

        System.out.println("Excel report generated successfully at: " + filePath);
        System.out.println("Execution ended for Excel report generation.");
    }
}
