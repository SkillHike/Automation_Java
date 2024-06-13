package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class ExcelReader {

    public static void main(String[] args) {
        // Example usage
        String filePath = "C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\Reportsheet\\";
        String reportName = "Book1.xlsx";
        ExcelReader reader = new ExcelReader();
        List<String> primaryKeys = reader.getPrimaryKeyColumnNames(filePath, reportName);
        System.out.println("Column Names with PrimaryKey 'yes': " + primaryKeys);

        boolean answer = reader.arePrimaryKeysPresentInBothSheets(
                "C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\data1",
                "C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\data2",
                reportName
        );
        System.out.println("The data is present in excel: " + answer);
    }

    public List<String> getPrimaryKeyColumnNames(String filePath, String reportName) {
        List<String> columnNames = new ArrayList<>();
        String fullPath = filePath + reportName;

        try {
            if (reportName.endsWith(".xlsx")) {
                columnNames = getExcelPrimaryKeyColumnNames(fullPath);
            } else if (reportName.endsWith(".txt")) {
                columnNames = getTextFilePrimaryKeyColumnNames(fullPath);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + reportName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return columnNames;
    }

    private List<String> getExcelPrimaryKeyColumnNames(String fullPath) throws IOException {
        List<String> columnNames = new ArrayList<>();

        try (FileInputStream file = new FileInputStream(fullPath);
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);

            int primaryKeyColumnIndex = -1;
            int columnNameIndex = -1;

            // Get the header row
            Row headerRow = sheet.getRow(0);
            Iterator<Cell> cellIterator = headerRow.cellIterator();

            // Find the index of PrimaryKey and ColumnName columns
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                String headerValue = cell.getStringCellValue();
                if ("PrimaryKey".equals(headerValue)) {
                    primaryKeyColumnIndex = cell.getColumnIndex();
                } else if ("ColumnName".equals(headerValue)) {
                    columnNameIndex = cell.getColumnIndex();
                }
            }

            if (primaryKeyColumnIndex == -1 || columnNameIndex == -1) {
                System.out.println("PrimaryKey or ColumnName column not found.");
                return columnNames;
            }

            // Iterate through rows and filter based on PrimaryKey column
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue; // Skip header row
                }

                Cell primaryKeyCell = row.getCell(primaryKeyColumnIndex);
                if (primaryKeyCell != null && "yes".equalsIgnoreCase(primaryKeyCell.getStringCellValue())) {
                    Cell columnNameCell = row.getCell(columnNameIndex);
                    if (columnNameCell != null) {
                        columnNames.add(columnNameCell.getStringCellValue());
                    }
                }
            }
        }

        return columnNames;
    }

    private List<String> getTextFilePrimaryKeyColumnNames(String fullPath) throws IOException {
        List<String> columnNames = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fullPath))) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                return columnNames; // No header line, return empty list
            }

            String[] headers = headerLine.split("\\t"); // Assuming tab-separated headers
            columnNames.addAll(Arrays.asList(headers));
        }

        return columnNames;
    }

    public boolean arePrimaryKeysPresentInBothSheets(String folder1, String folder2, String fileName) {
        String filePath1 = folder1 + "\\" + fileName;
        String filePath2 = folder2 + "\\" + fileName;

        try {
            Set<String> primaryKeys1 = getPrimaryKeys(filePath1);
            Set<String> primaryKeys2 = getPrimaryKeys(filePath2);

            return primaryKeys1.equals(primaryKeys2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Set<String> getPrimaryKeys(String filePath) throws IOException {
        if (filePath.endsWith(".xlsx")) {
            return getPrimaryKeysFromSheet(filePath);
        } else if (filePath.endsWith(".txt")) {
            return getPrimaryKeysFromTextFile(filePath);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + filePath);
        }
    }

    private Set<String> getPrimaryKeysFromSheet(String filePath) throws IOException {
        Set<String> primaryKeys = new HashSet<>();

        try (FileInputStream file = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return primaryKeys; // No header row, return empty set
            }

            Iterator<Cell> cellIterator = headerRow.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                primaryKeys.add(cell.getStringCellValue());
            }
        }

        return primaryKeys;
    }

    private Set<String> getPrimaryKeysFromTextFile(String filePath) throws IOException {
        Set<String> primaryKeys = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                return primaryKeys; // No header line, return empty set
            }

            String[] headers = headerLine.split("\\t");
            for (String header : headers) {
                primaryKeys.add(header.trim());
            }
        }

        return primaryKeys;
    }
}
