package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

        // Get tolerance values for primary key column names from Excel
        Map<String, String> columnNameToTolerance = getToleranceValuesForPrimaryKeys("C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\Reportsheet\\Book1.xlsx", primaryKeys);

        // Iterate over the reportData in chunks of 4 (column names, dataInEnv1, dataInEnv2, difference)
        for (int i = 0; i < reportData.size(); i += 4) {
            if (i + 3 >= reportData.size()) {
                System.out.println("Skipping incomplete set of rows at index: " + i);
                break; // Skip incomplete sets of rows
            }

            String[] columnNames = reportData.get(i);
            String[] dataInEnv1 = reportData.get(i + 3);
            String[] dataInEnv2 = reportData.get(i + 4);
            String[] difference = reportData.get(i + 5);

            // Check if there's an actual difference
            boolean hasDifference = false;
            for (int j = 1; j < difference.length; j++) {
                if (!"matched".equals(difference[j]) && !"".equals(difference[j].trim())) {
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
                        differenceValue = Math.abs(differenceValue); // Convert negative difference to positive
                        String cellStyle = getDifferenceCellStyle(differenceValue);
                        htmlBuilder.append("<td style='").append(cellStyle).append("'>").append(cell).append("</td>");
                    } else {
                        htmlBuilder.append("<td></td>"); // Skip matched or blank cell
                    }
                }
            }
            htmlBuilder.append("</tr>");

            // Tolerance row
            htmlBuilder.append("<tr>");
            htmlBuilder.append("<td>").append("Tolerance").append("</td>");
            boolean toleranceRowPrinted = false; // Track if the tolerance row is printed
            for (int j = 0; j < difference.length; j++) {
                if (!removeColumn[j]) {
                    String columnName = columnNames[j];
                    String toleranceValue = columnNameToTolerance.get(columnName);
                    if (toleranceValue != null && !"No".equalsIgnoreCase(toleranceValue.trim()) && !"".equals(difference[j].trim()) && !"matched".equals(difference[j])) {
                        double differenceValue = parseDouble(difference[j]);
                        differenceValue = Math.abs(differenceValue); // Convert negative difference to positive

                        String toleranceLabel = getToleranceLabel(differenceValue);
                        String cellStyle = getToleranceCellStyle(toleranceValue,toleranceLabel);
                        String toleranceresult= "Na";
                        if(toleranceValue.equalsIgnoreCase(toleranceLabel)){
                            toleranceresult= "Matched with tolerance";
                        }else{
                           toleranceresult= "Not-Matched with tolerance";
                        }
                        htmlBuilder.append("<td style='").append(cellStyle).append("'>").append(toleranceresult).append("</td>");
                        toleranceRowPrinted = true; // Set flag to true when tolerance value is printed
                    } else {
                        htmlBuilder.append("<td></td>"); // Skip blank cell
                    }
                }
            }
            htmlBuilder.append("</tr>");

            // Only add an empty row if the tolerance row was printed
            if (toleranceRowPrinted) {
                htmlBuilder.append("<tr><td colspan='").append(columnNames.length + 1).append("'></td></tr>"); // Empty row after each set of rows
            }
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


    public static void generateExcelReport(String filePath, List<String[]> reportData, Set<String> primaryKeys) throws IOException {
        if (reportData.isEmpty()) {
            System.out.println("No data to generate report. Excel report will not be generated.");
            return;
        }

        System.out.println("Execution started for Excel report generation.");

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Comparison Results");

        // Create reusable cell styles
        CellStyle greenStyle = createGreenStyle(workbook);
        CellStyle yellowStyle = createYellowStyle(workbook);
        CellStyle redStyle = createRedStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);

        // Get tolerance values for columns where tolerance is not "No"
        Map<String, String> columnNameToTolerance = getToleranceValuesForPrimaryKeys("C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\Reportsheet\\Book1.xlsx", primaryKeys);

        int rowNum = 0;

        for (int i = 0; i < reportData.size(); i += 4) {
            if (i + 3 >= reportData.size()) {
                System.out.println("Skipping incomplete set of rows at index: " + i);
                break; // Skip incomplete sets of rows
            }

            String[] tradeIdRow = reportData.get(i);
            String[] dataInEnv1 = reportData.get(i + 3);
            String[] dataInEnv2 = reportData.get(i + 4);
            String[] differenceRow = reportData.get(i + 5);

            // Check if there's an actual difference
            boolean hasDifference = false;
            for (int j = 1; j < differenceRow.length; j++) {
                if (!"matched".equals(differenceRow[j]) && !"".equals(differenceRow[j].trim())) {
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

            // Column names row
            Cell columnNameHeaderCell = tradeIdExcelRow.createCell(cellNum);
            columnNameHeaderCell.setCellValue("Column names");
            columnNameHeaderCell.setCellStyle(boldStyle);

            // Data in Env1 row
            Cell dataInEnv1HeaderCell = dataInEnv1ExcelRow.createCell(cellNum);
            dataInEnv1HeaderCell.setCellValue("Data in Env1");
            dataInEnv1HeaderCell.setCellStyle(boldStyle);

            // Data in Env2 row
            Cell dataInEnv2HeaderCell = dataInEnv2ExcelRow.createCell(cellNum);
            dataInEnv2HeaderCell.setCellValue("Data in Env2");
            dataInEnv2HeaderCell.setCellStyle(boldStyle);

            // Difference row
            Cell differenceHeaderCell = differenceExcelRow.createCell(cellNum);
            differenceHeaderCell.setCellValue("Difference");
            differenceHeaderCell.setCellStyle(boldStyle);

            // Tolerance row
            Cell toleranceHeaderCell = toleranceExcelRow.createCell(cellNum);
            toleranceHeaderCell.setCellValue("Tolerance");
            toleranceHeaderCell.setCellStyle(boldStyle);

            boolean toleranceRowPrinted = false; // Track if the tolerance row is printed

            for (int j = 1; j < differenceRow.length; j++) {
                if (!"matched".equals(differenceRow[j]) && !"".equals(differenceRow[j].trim())) {
                    cellNum++;
                    Cell headerCell = tradeIdExcelRow.createCell(cellNum);
                    headerCell.setCellValue(tradeIdRow[j]);
                    headerCell.setCellStyle(boldStyle);

                    dataInEnv1ExcelRow.createCell(cellNum).setCellValue(dataInEnv1[j]);
                    dataInEnv2ExcelRow.createCell(cellNum).setCellValue(dataInEnv2[j]);
                    Cell diffCell = differenceExcelRow.createCell(cellNum);
                    diffCell.setCellValue(differenceRow[j]);

                    double differenceValue = parseDouble(differenceRow[j]);
                    differenceValue = Math.abs(differenceValue); // Convert negative difference to positive

                    String toleranceLabel = columnNameToTolerance.getOrDefault(tradeIdRow[j], "");
                    Cell toleranceCell = toleranceExcelRow.createCell(cellNum);
                    String toleranceresult="";

                    if (!"No".equalsIgnoreCase(toleranceLabel)) {
                        if("low".equalsIgnoreCase(toleranceLabel)){
                        if (differenceValue < 0.5) {
                            toleranceresult = "Matched with tolerance";
                            toleranceCell.setCellStyle(greenStyle);
                            }
                        } else if("Medium".equalsIgnoreCase(toleranceLabel)){
                        if(differenceValue > 0.5 && differenceValue <= 1) {
                            toleranceresult = "Matched with tolerance";
                            toleranceCell.setCellStyle(greenStyle);
                            }
                        } else if ("High".equalsIgnoreCase(toleranceLabel)){
                        if (differenceValue > 1) {
                            toleranceresult = "Matched with tolerance";
                            toleranceCell.setCellStyle(greenStyle);

                            }
                        }else {
                            toleranceresult = "Not-matched with tolerance";
                            toleranceCell.setCellStyle(redStyle);

                        }
                    }
                    toleranceCell.setCellValue(toleranceresult);

                    toleranceRowPrinted = true; // Set flag to true when tolerance value is printed
                }
            }

            // Only add an empty row if the tolerance row was printed
            if (toleranceRowPrinted) {
                sheet.createRow(rowNum++); // Empty row
            }
        }

        // Set column widths
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }

        Files.createDirectories(Paths.get(filePath).getParent());
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            workbook.write(outputStream);
        }

        System.out.println("Excel report generated successfully at: " + filePath);
        System.out.println("Execution ended for Excel report generation.");
    }

    // Helper method to create green fill style
    private static CellStyle createGreenStyle(Workbook workbook) {
        CellStyle greenStyle = workbook.createCellStyle();
        greenStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
        greenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return greenStyle;
    }

    // Helper method to create yellow fill style
    private static CellStyle createYellowStyle(Workbook workbook) {
        CellStyle yellowStyle = workbook.createCellStyle();
        yellowStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return yellowStyle;
    }

    // Helper method to create red fill style
    private static CellStyle createRedStyle(Workbook workbook) {
        CellStyle redStyle = workbook.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return redStyle;
    }

    // Helper method to create bold font style
    private static CellStyle createBoldStyle(Workbook workbook) {
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        return boldStyle;
    }

    // Example method for getting tolerance values for columns where tolerance is not "No"
    private static Map<String, String> getToleranceValuesForPrimaryKeys(String excelFilePath, Set<String> primaryKeys) throws IOException {
        Map<String, String> columnNameToTolerance = new HashMap<>();

        try (FileInputStream file = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);  // Assuming data is in the first sheet
            int toleranceIdx = 5; // Column index for Tolerance
            int columnNameIdx = 1; // Column index for ColumnName

            // Process the rows starting after the header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell columnNameCell = row.getCell(columnNameIdx);
                    Cell toleranceCell = row.getCell(toleranceIdx);

                    if (columnNameCell != null && toleranceCell != null) {
                        String columnName = columnNameCell.getStringCellValue().trim();
//                        if (primaryKeys.contains(columnName)) {
                            String toleranceValue = "";
                            if (toleranceCell.getCellType() == CellType.STRING) {
                                toleranceValue = toleranceCell.getStringCellValue().trim();
                            }
                            columnNameToTolerance.put(columnName, toleranceValue);
                        }
                    }
                }
            }
//        }

        return columnNameToTolerance;
    }

    // Example method for converting difference value to tolerance label
    private static String getToleranceLabel(double differenceValue) {
        if (differenceValue < 0.5) {
            return "low";
        } else if (differenceValue < 1.0) {
            return "medium";
        } else {
            return "high";
        }
    }
    private static String getToleranceCellStyle(String tolerancevalue,String tolerancelabel) {
//        differenceValue = Math.abs(differenceValue); // Convert to absolute value
        if(tolerancelabel.equalsIgnoreCase(tolerancevalue))
        {return "background-color: green;";}
        else{ return "background-color: red;";}

    }

    private static String getDifferenceCellStyle(double differenceValue) {
        if (differenceValue < 0.5) {
            return "background-color: green;";
        } else if (differenceValue < 1.0) { // Adjusted threshold for medium
            return "background-color: yellow;";
        } else {return "background-color: red;";

        }
    }



    // Example method for parsing double from string
    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}

