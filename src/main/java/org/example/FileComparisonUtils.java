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

        int numRows = Math.max(file1Data.size(), file2Data.size());
        List<String> headers = file1Data.get(0);
        int numCols = headers.size();

        for (int i = 1; i < numRows; i++) {
            List<String> row1 = i < file1Data.size() ? file1Data.get(i) : new ArrayList<>();
            List<String> row2 = i < file2Data.size() ? file2Data.get(i) : new ArrayList<>();

            String tradeId = i < file1Data.size() ? row1.get(0) : row2.get(0);
            String[] tradeIdRow = new String[numCols];
            tradeIdRow[0] = tradeId;
            for (int j = 1; j < numCols; j++) {
                tradeIdRow[j] = headers.get(j);
            }
            results.add(tradeIdRow);

            results.add(createRowData("Data in Env1", row1, row2, numCols));
            results.add(createRowData("Data in Env2", row2, row1, numCols));
            results.add(createDifferenceRow(row1, row2, numCols));
        }

        return results;
    }

    private static String[] createRowData(String label, List<String> rowData, List<String> comparisonData, int numCols) {
        String[] row = new String[numCols];
        row[0] = label;
        for (int j = 1; j < numCols; j++) {
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
        for (int j = 1; j < numCols; j++) {
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
