package org.example;

import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


public class ConfigurationFileReader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationFileReader.class);

    public static List<String> readPrimaryKeyColumns(String filePath, String filename) throws IOException {
        List<String> primaryKeyColumns = new ArrayList<>();

        // Get the path of the current file
        Path file = Paths.get(filePath, filename);

        // Check if the file exists
        if (Files.exists(file)) {
            if (filename.endsWith(".xlsx")) {
                readPrimaryKeyColumnsFromExcel(file, primaryKeyColumns);
            } else if (filename.endsWith(".txt")) {
                readPrimaryKeyColumnsFromText(file, primaryKeyColumns);
            } else {
                logger.error("Unsupported file type: {}", file);
            }
        } else {
            logger.error("The file {} does not exist.", file);
        }

        return primaryKeyColumns;
    }

    private static void readPrimaryKeyColumnsFromExcel(Path file, List<String> primaryKeyColumns) {
        try (InputStream is = Files.newInputStream(file);
             Workbook workbook = new XSSFWorkbook(is)) {

            // Iterate through all sheets in the workbook
            for (Sheet sheet : workbook) {
                boolean primaryKeyRowFound = false;
                for (Row row : sheet) {
                    // Check if the value in the "PrimaryKey" column is "yes"
                    Cell primaryKeyCell = row.getCell(3); // Assuming "PrimaryKey" column is at index 3
                    if (primaryKeyCell != null && "yes".equalsIgnoreCase(primaryKeyCell.getStringCellValue())) {
                        primaryKeyRowFound = true;
                    }
                    // If primary key row is found, add the corresponding column name to the list
                    if (primaryKeyRowFound) {
                        Cell columnNameCell = row.getCell(1); // Assuming "ColumnName" column is at index 1
                        if (columnNameCell != null && !columnNameCell.getStringCellValue().isEmpty()) {
                            primaryKeyColumns.add(columnNameCell.getStringCellValue());
                        }
                    }
                }
                // Exit loop after finding the primary key columns in the first sheet
                if (!primaryKeyColumns.isEmpty()) {
                    break;
                }
            }

        } catch (NotOfficeXmlFileException e) {
            logger.error("The file {} is not a valid OOXML file.", file);
        } catch (IOException e) {
            logger.error("Error reading the Excel file: {}", file, e);
        }
    }

    private static void readPrimaryKeyColumnsFromText(Path file, List<String> primaryKeyColumns) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            boolean primaryKeyRowFound = false;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\t"); // Assuming tab-separated values
                // Check if the value in the "PrimaryKey" column is "yes"
                if (columns.length > 3 && "yes".equalsIgnoreCase(columns[3])) { // Assuming "PrimaryKey" column is at index 3
                    primaryKeyRowFound = true;
                }
                // If primary key row is found, add the corresponding column name to the list
                if (primaryKeyRowFound) {
                    if (columns.length > 1 && !columns[1].isEmpty()) { // Assuming "ColumnName" column is at index 1
                        primaryKeyColumns.add(columns[1]);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading the text file: {}", file, e);
        }
    }}
