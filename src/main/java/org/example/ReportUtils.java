package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ReportUtils {

    public static void generateHTMLReport(String filePath, List<String[]> reportData) throws IOException {
        if (reportData.isEmpty()) {
            System.out.println("No data to generate report. HTML report will not be generated.");
            return;
        }

        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<html><head><title>Comparison Report</title></head><body>");
        htmlBuilder.append("<h1>Comparison Report</h1>");
        htmlBuilder.append("<table border='1'>");

        // Add header row
        htmlBuilder.append("<tr><th>TradeID</th><th>Data Matched</th><th>Data in Data1</th><th>Data in Data2</th><th>Difference</th></tr>");

        for (String[] rowData : reportData) {
            String tradeId = rowData[0];
            String matched = rowData[1];
            String dataInData1 = rowData[2];
            String dataInData2 = rowData[3];
            String difference = rowData[4];

            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append(tradeId).append("</td>");
            if (matched.equals("No")) {
                htmlBuilder.append("<td style='background-color:red;'>").append(matched).append("</td>");
            } else {
                htmlBuilder.append("<td style='background-color:green;'>").append(matched).append("</td>");
            }
            htmlBuilder.append("<td>").append(dataInData1).append("</td>");
            htmlBuilder.append("<td>").append(dataInData2).append("</td>");
            htmlBuilder.append("<td>").append(difference).append("</td>");
            htmlBuilder.append("</tr>");
        }

        htmlBuilder.append("</table></body></html>");

        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(htmlBuilder.toString().getBytes());
        }

        System.out.println("HTML report generated successfully at: " + filePath);
    }

    public static void generateExcelReport(String filePath, List<String[]> reportData) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Comparison Results");
        int rowCount = 0;

        CellStyle redStyle = workbook.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font whiteFont = workbook.createFont();
        whiteFont.setColor(IndexedColors.WHITE.getIndex());
        redStyle.setFont(whiteFont);

        CellStyle greenStyle = workbook.createCellStyle();
        greenStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        greenStyle.setFont(whiteFont);

        // Add header row
        Row headerRow = sheet.createRow(rowCount++);
        String[] headers = {"TradeID", "Data Matched", "Data in Data1", "Data in Data2", "Difference"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        for (String[] rowData : reportData) {
            Row row = sheet.createRow(rowCount++);
            String tradeId = rowData[0];
            String matched = rowData[1];
            String dataInData1 = rowData[2];
            String dataInData2 = rowData[3];
            String difference = rowData[4];

            int columnCount = 0;
            Cell tradeIdCell = row.createCell(columnCount++);
            tradeIdCell.setCellValue(tradeId);

            Cell matchedCell = row.createCell(columnCount++);
            matchedCell.setCellValue(matched);
            if (matched.equals("No")) {
                matchedCell.setCellStyle(redStyle);
            } else {
                matchedCell.setCellStyle(greenStyle);
            }

            Cell data1Cell = row.createCell(columnCount++);
            data1Cell.setCellValue(dataInData1);

            Cell data2Cell = row.createCell(columnCount++);
            data2Cell.setCellValue(dataInData2);

            Cell differenceCell = row.createCell(columnCount++);
            differenceCell.setCellValue(difference);
        }

        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }
        workbook.close();
        System.out.println("Excel report generated successfully at: " + filePath);
    }
}
