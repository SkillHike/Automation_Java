package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            if (isFileSizeExceeded(fullPath, 15)) {
                System.out.println("File size exceeds the limit of 15MB: " + reportName);
                return columnNames;
            }

            if (reportName.endsWith(".xlsx")) {
                columnNames = getExcelPrimaryKeyColumnNames(fullPath);
            } else if (reportName.endsWith(".txt")) {
                columnNames = getTextFilePrimaryKeyColumnNames(fullPath);
            } else if (reportName.endsWith(".csv")) {
                columnNames = getCSVFilePrimaryKeyColumnNames(fullPath);
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + reportName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return columnNames;
    }

    private boolean isFileSizeExceeded(String filePath, int sizeLimitMB) throws IOException {
        File file = new File(filePath);
        long fileSizeInBytes = Files.size(Paths.get(filePath));
        long fileSizeInMB = fileSizeInBytes / (1024 * 1024);
        return fileSizeInMB > sizeLimitMB;
    }

    public List<String> getExcelPrimaryKeyColumnNames(String fullPath) throws IOException {
        List<String> columnNames = new ArrayList<>();


        try (FileInputStream file = new FileInputStream("C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\Reportsheet\\Book1.xlsx");

             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);  // Assuming data is in the first sheet
            int comparisionRequiredIdx = 4; // Column index for ComparisionRequired
            int primaryKeyIdx = 3; // Column index for PrimaryKey
            int columnNameIdx = 1; // Column index for ColumnName

            // Identify the column indices based on the header row
            Row headerRow = sheet.getRow(0);

            // Process the rows starting after the header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell comparisionRequiredCell = row.getCell(comparisionRequiredIdx);
                    Cell primaryKeyCell = row.getCell(primaryKeyIdx);
                    Cell columnNameCell = row.getCell(columnNameIdx);

                    if (comparisionRequiredCell != null && primaryKeyCell != null && columnNameCell != null) {
                        String comparisionRequired = comparisionRequiredCell.getStringCellValue();
                        String primaryKey = primaryKeyCell.getStringCellValue();

                        if ("yes".equalsIgnoreCase(comparisionRequired) && "yes".equalsIgnoreCase(primaryKey)) {
                            columnNames.add(columnNameCell.getStringCellValue());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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

    private List<String> getCSVFilePrimaryKeyColumnNames(String fullPath) throws IOException {
        List<String> columnNames = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fullPath))) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                return columnNames; // No header line, return empty list
            }

            String[] headers = headerLine.split(","); // Assuming comma-separated headers
            columnNames.addAll(Arrays.asList(headers));
        }

        return columnNames;
    }

    public boolean arePrimaryKeysPresentInBothSheets(String folder1, String folder2, String fileName) {
        String filePath1 = folder1 + "\\" + fileName;
        String filePath2 = folder2 + "\\" + fileName;

        try {
            if (isFileSizeExceeded(filePath1, 15)) {
                System.out.println("File size exceeds the limit of 15MB: " + filePath1);
                return false;
            }
            if (isFileSizeExceeded(filePath2, 15)) {
                System.out.println("File size exceeds the limit of 15MB: " + filePath2);
                return false;
            }

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
        } else if (filePath.endsWith(".csv")) {
            return getPrimaryKeysFromCSVFile(filePath);
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

    private Set<String> getPrimaryKeysFromCSVFile(String filePath) throws IOException {
        Set<String> primaryKeys = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                return primaryKeys; // No header line, return empty set
            }

            String[] headers = headerLine.split(",");
            for (String header : headers) {
                primaryKeys.add(header.trim());
            }
        }

        return primaryKeys;
    }
}
