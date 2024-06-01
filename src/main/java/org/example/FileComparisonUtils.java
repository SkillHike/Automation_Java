package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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
        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(filePath)))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(cell.toString());
                }
                records.add(rowData);
            }
        }
        return records;
    }

    public static List<String[]> compareFiles(List<List<String>> file1Data, List<List<String>> file2Data) {
        List<String[]> results = new ArrayList<>();

        // Ensure both files have the same number of rows and columns
        int numRows = Math.min(file1Data.size(), file2Data.size());
        List<String> headers = file1Data.get(0);
        int numCols = headers.size();

        for (int i = 1; i < numRows; i++) { // Start from 1 to skip headers
            List<String> row1 = file1Data.get(i);
            List<String> row2 = file2Data.get(i);
            String tradeId = row1.get(0);
            boolean matched = true;

            for (int j = 1; j < numCols; j++) { // Start from 1 to skip TradeID column
                if (!row1.get(j).equals(row2.get(j))) {
                    String columnName = headers.get(j);
                    String cellLocation = String.format("(%s)", columnName);
                    String difference = "";

                    if (isNumeric(row1.get(j)) && isNumeric(row2.get(j))) {
                        double value1 = Double.parseDouble(row1.get(j));
                        double value2 = Double.parseDouble(row2.get(j));
                        difference = String.valueOf(value1 - value2);
                    }

                    results.add(new String[]{
                            tradeId,
                            "No",
                            row1.get(j) + " " + cellLocation,
                            row2.get(j) + " " + cellLocation,
                            difference
                    });
                    matched = false;
                }
            }

            if (matched) {
                results.add(new String[]{
                        tradeId,
                        "Yes",
                        "", // Empty cell for mismatched data in Data1
                        "",  // Empty cell for mismatched data in Data2
                        ""   // Empty cell for difference
                });
            }
        }

        return results;
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
