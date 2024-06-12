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

        return performComparison(file1Map, file2Map, primaryKeyColumns1, primaryKeyColumns2, headerMap1, headerMap2);
    }


    private static List<String[]> performComparison(Map<String, List<String>> file1Map, Map<String, List<String>> file2Map, List<String> primaryKeyColumns1, List<String> primaryKeyColumns2, Map<String, Integer> headerMap1, Map<String, Integer> headerMap2) {
        List<String[]> differences = new ArrayList<>();

        // Check for rows in file1 but not in file2
        for (Map.Entry<String, List<String>> entry : file1Map.entrySet()) {
            String key = entry.getKey();
            if (!file2Map.containsKey(key)) {
                differences.add(new String[]{"Missing in File2", key, entry.getValue().toString()});
            } else {
                List<String> row1 = entry.getValue();
                List<String> row2 = file2Map.get(key);

                // Compare using primary key columns for each file
                for (String primaryKey : primaryKeyColumns1) {
                    Integer index1 = headerMap1.get(primaryKey);
                    Integer index2 = headerMap2.get(primaryKey);

                    if (index1 != null && index2 != null && !row1.get(index1).equals(row2.get(index2))) {
                        differences.add(new String[]{"Difference", key, primaryKey, row1.get(index1), row2.get(index2)});
                    }
                }
            }
        }

        // Check for rows in file2 but not in file1
        for (Map.Entry<String, List<String>> entry : file2Map.entrySet()) {
            String key = entry.getKey();
            if (!file1Map.containsKey(key)) {
                differences.add(new String[]{"Missing in File1", key, entry.getValue().toString()});
            }
        }

        return differences;
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

        for (List<String> row : data) {
            if (row.size() >= 3 && "yes".equalsIgnoreCase(row.get(2))) { // Check if column 3 has "yes"
                String key = row.get(1); // Pick the value from column 2 as the primary key
                System.out.println("key=" + key + "  row=" + row);
                dataMap.put(key, row);
            }
        }

        return dataMap;
    }
}
