package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
        try (InputStream is = Files.newInputStream(Paths.get(filePath));
             Workbook workbook = new XSSFWorkbook(is)) {
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


    public static List<String[]> compareFiles(List<List<String>> file1Data, List<List<String>> file2Data) {
        List<String[]> results = new ArrayList<>();

        // Determine number of rows and columns based on file1Data (assuming file1Data contains headers)
        int numRows = Math.max(file1Data.size(), file2Data.size());
        List<String> headers = file1Data.get(0); // Assuming headers are in the first row of file1Data
        int numCols = headers.size();

        // Iterate through the rows (excluding headers) of file1Data and file2Data
        for (int i = 1; i < numRows; i++) {
            List<String> row1 = i < file1Data.size() ? file1Data.get(i) : new ArrayList<>();
            List<String> row2 = i < file2Data.size() ? file2Data.get(i) : new ArrayList<>();

            // Extract trade ID (assuming it's the first column in both file1Data and file2Data)
            String tradeId = i < file1Data.size() ? file1Data.get(i).get(0) : file2Data.get(i).get(0);

            // Create trade data row and add to results
            results.add(createTradeData(headers, tradeId, row1, row2, numCols));

            // Add data in environment 1 (row1) to results
            results.add(createRowData("Data in Env1", row1, row2, numCols));

            // Add data in environment 2 (row2) to results
            results.add(createRowData("Data in Env2", row2, row1, numCols));

            // Add difference row between row1 and row2 to results
            results.add(createDifferenceRow(row1, row2, numCols));
        }

        return results;
    }

    // Method to create trade data row based on column headers, trade ID, and row data from both environments
    private static String[] createTradeData(List<String> headers, String tradeId, List<String> row1, List<String> row2, int numCols) {
        String[] tradeDataRow = new String[numCols];


        // Set column names as the rest of the elements in the row
        for (int j = 0; j < numCols; j++) {
            tradeDataRow[j] = headers.get(j);
        }

        return tradeDataRow;
    }

    private static String[] createRowData(String label, List<String> rowData, List<String> comparisonData, int numCols) {
        String[] row = new String[numCols];
        row[0] = label;
        for (int j = 0; j < numCols; j++) {
            String cellValue = rowData.size() > j ? rowData.get(j) : "";
            String compareValue = comparisonData.size() > j ? comparisonData.get(j) : "";
            if (cellValue.equals(compareValue)) {
                row[j] = cellValue;
            } else {
                row[j] = cellValue;
            }
        }
        return row;
    }

    private static String[] createDifferenceRow(List<String> row1, List<String> row2, int numCols) {
        String[] row = new String[numCols];
        row[0] = "Difference";
        for (int j = 0; j < numCols; j++) {
            if (j < row1.size() && j < row2.size()) {
                double value1 = parseDouble(row1.get(j));
                double value2 = parseDouble(row2.get(j));
                if (!Double.isNaN(value1) && !Double.isNaN(value2)) {
                    double difference = value1 - value2;
                    if (Math.abs(difference) > 0.5) {
                        row[j] = String.valueOf(difference);
                    } else {
                        row[j] = "matched";
                    }
                } else if (row1.get(j).equals(row2.get(j))) {
                    row[j] = "matched";
                } else {
                    row[j] = row1.get(j) + " | " + row2.get(j);
                }
            } else {
                row[j] = "matched";
            }
        }
        return row;
    }



    private static double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
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
