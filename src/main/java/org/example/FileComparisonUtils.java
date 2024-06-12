package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileComparisonUtils {

    public static List<List<String>> readCSV(String filePath) throws IOException, CsvValidationException {
        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                List<String> row = new ArrayList<>();
                for (String value : values) {
                    row.add(value);
                }
                records.add(row);
            }
        }
        return records;
    }

    public static List<List<String>> readExcel(String filePath) throws IOException {
        List<List<String>> records = new ArrayList<>();
        String extension = getFileExtension(filePath);
        if (!"xlsx".equalsIgnoreCase(extension)) {
            throw new IllegalArgumentException("Invalid file extension: " + extension);
        }

        System.out.println("Reading Excel file: " + filePath);

        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    switch (cell.getCellType()) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            rowData.add(String.valueOf(cell.getNumericCellValue()));
                            break;
                        case BOOLEAN:
                            rowData.add(String.valueOf(cell.getBooleanCellValue()));
                            break;
                        case FORMULA:
                            rowData.add(cell.getCellFormula());
                            break;
                        default:
                            rowData.add("");
                    }
                }
                records.add(rowData);
            }
        } catch (NotOfficeXmlFileException e) {
            System.err.println("The file is not a valid Excel file: " + filePath);
            throw e;
        }

        return records;
    }

    public static List<List<String>> readTextFile(String filePath) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        for (String line : lines) {
            List<String> rowData = new ArrayList<>();
            for (String value : line.split("\\t")) {  // Assuming tab-separated values
                rowData.add(value);
            }
            records.add(rowData);
        }
        return records;
    }

    private static String getFileExtension(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            int lastIndex = filePath.lastIndexOf(".");
            if (lastIndex != -1 && lastIndex != 0 && lastIndex < filePath.length() - 1) {
                return filePath.substring(lastIndex + 1);
            }
        }
        return "";
    }

    public static List<String[]> compareFiles(List<List<String>> file1Data, List<List<String>> file2Data, List<String> primaryKeyColumns1, List<String> primaryKeyColumns2) {
        Map<String, Integer> headerMap1 = buildHeaderMap(file1Data.get(0));
        Map<String, Integer> headerMap2 = buildHeaderMap(file2Data.get(0));

        // Check if the first value is "Columnnames" and remove it
        if (!primaryKeyColumns1.isEmpty() && primaryKeyColumns1.get(0).equals("ColumnName")) {
            primaryKeyColumns1.remove(0);
        }
        if (!primaryKeyColumns2.isEmpty() && primaryKeyColumns2.get(0).equals("ColumnName")) {
            primaryKeyColumns2.remove(0);
        }

        // Ensure the primary key columns exist in both files
        for (String primaryKey : primaryKeyColumns1) {
            if (!headerMap1.containsKey(primaryKey)) {
                throw new IllegalArgumentException("Primary key column " + primaryKey + " does not exist in file 1.");
            }
        }
        for (String primaryKey : primaryKeyColumns2) {
            if (!headerMap2.containsKey(primaryKey)) {
                throw new IllegalArgumentException("Primary key column " + primaryKey + " does not exist in file 2.");
            }
        }

        Map<String, List<String>> file1Map = buildDataMap(file1Data);
        Map<String, List<String>> file2Map = buildDataMap(file2Data);
        System.out.println(file1Data+"-------filemap---------"+file2Data);

        return performComparison(file1Map, file2Map, primaryKeyColumns1);
    }

    private static List<String[]> performComparison(Map<String, List<String>> file1Map, Map<String, List<String>> file2Map, List<String> primaryKeyColumns) {
        List<String[]> differences = new ArrayList<>();

        // Ensure that both maps are not empty
        if (file1Map.isEmpty() || file2Map.isEmpty()) {
            System.err.println("One or both of the maps are empty.");
            return differences;
        }

        // Compare column values and calculate differences
        Map<String, Integer> headerMap1 = buildHeaderMap(file1Map.get(file1Map.keySet().iterator().next()));
        Map<String, Integer> headerMap2 = buildHeaderMap(file2Map.get(file2Map.keySet().iterator().next()));

        for (Map.Entry<String, List<String>> entry : file1Map.entrySet()) {
            String key = entry.getKey();
            if (file2Map.containsKey(key)) {
                List<String> row1 = entry.getValue();
                List<String> row2 = file2Map.get(key);

                for (String columnName : headerMap1.keySet()) {
                    Integer index1 = headerMap1.get(columnName);
                    Integer index2 = headerMap2.get(columnName);

                    // Check if both indexes are not null
                    if (index1 != null && index2 != null) {
                        // Check if the rows have enough columns
                        if (index1 < row1.size() && index2 < row2.size()) {
                            String value1 = row1.get(index1);
                            String value2 = row2.get(index2);

                            // Compare values based on their types
                            if (isNumeric(value1) && isNumeric(value2)) {
                                double numericValue1 = Math.abs(Double.parseDouble(value1));
                                double numericValue2 = Math.abs(Double.parseDouble(value2));
                                double difference = numericValue1 - numericValue2;
                                String[] differenceEntry = {key, columnName, String.valueOf(difference)};
                                differences.add(differenceEntry);
                            } else {
                                if (!value1.equals(value2)) {
                                    // Values are not equal, so store the difference
                                    String[] differenceEntry = {key, columnName, "Values are not equal"};
                                    differences.add(differenceEntry);
                                }
                            }
                        } else {
                            // Handle the case where the rows do not have enough columns
                            System.err.println("Row does not have enough columns for comparison: " + columnName);
                        }
                    } else {
                        // Handle the case where the column is not present in one of the files
                        System.err.println("Column not found in one of the files: " + columnName);
                    }
                }
            }
        }

        return differences;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }




    private static Map<String, Integer> buildHeaderMap(List<String> headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            headerMap.put(headerRow.get(i), i);
        }
        return headerMap;
    }

    private static Map<String, List<String>> buildDataMap(List<List<String>> data) {
        Map<String, List<String>> dataMap = new HashMap<>();

        // Assuming the first row contains the header, so we skip it
        for (int i = 1; i < data.size(); i++) {
            List<String> row = data.get(i);

            // Check if the row has enough elements
            if (row == null || row.size() < 4) { // Adjusted to 4 since your data seems to have 4 columns
                System.err.println("Row is null or does not have enough elements: " + row);
                continue; // Skip this row
            }

            String key = row.get(0); // Assuming the key is in the first column
            List<String> rowData = new ArrayList<>(row.subList(1, row.size()));

            // Convert the string representation of numbers to doubles
            for (int j = 0; j < rowData.size(); j++) {
                try {
                    Double.parseDouble(rowData.get(j));
                } catch (NumberFormatException e) {
                    System.err.println("Invalid numeric value: " + rowData.get(j));
                    rowData.set(j, "0.0"); // Set to a default value or handle the error accordingly
                }
            }

            // Put the data into the map
            dataMap.put(key, rowData);
        }

        return dataMap;
    }



}
