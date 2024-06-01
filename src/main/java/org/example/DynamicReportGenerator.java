package org.example;

import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DynamicReportGenerator {

    public static void generateReports(String folder1, String folder2, String baseOutputPath) {
        try {
            // Iterate over files in folder1
            Files.list(Paths.get(folder1)).forEach(file1Path -> {
                try {
                    Path file2Path = Paths.get(folder2, file1Path.getFileName().toString());
                    if (Files.exists(file2Path)) {
                        // Initialize report data for each file
                        List<String[]> reportData = new ArrayList<>();

                        // Compare files
                        List<List<String>> file1Data = file1Path.toString().endsWith(".csv") ? FileComparisonUtils.readCSV(file1Path.toString()) : FileComparisonUtils.readExcel(file1Path.toString());
                        List<List<String>> file2Data = file2Path.toString().endsWith(".csv") ? FileComparisonUtils.readCSV(file2Path.toString()) : FileComparisonUtils.readExcel(file2Path.toString());
                        List<String[]> mismatches = FileComparisonUtils.compareFiles(file1Data, file2Data);

                        // Add comparison result to report data, excluding the heading row
                        if (!mismatches.isEmpty()) {
                            reportData.addAll(mismatches);
                        }

                        // Create output directory with timestamp
                        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                        String fileBaseName = file1Path.getFileName().toString().replace(".", "_");
                        Path outputDir = Paths.get(baseOutputPath, fileBaseName + "_" + timeStamp);
                        Files.createDirectories(outputDir);

                        // Generate Excel report
                        String excelFilePath = outputDir.resolve(fileBaseName + ".xlsx").toString();
                        ReportUtils.generateExcelReport(excelFilePath, reportData);
                        System.out.println("Excel report generated successfully at: " + excelFilePath);

                        // Generate HTML report
                        String htmlFilePath = outputDir.resolve(fileBaseName + ".html").toString();
                        ReportUtils.generateHTMLReport(htmlFilePath, reportData);
                        System.out.println("HTML report generated successfully at: " + htmlFilePath);

                    } else {
                        // Handle missing file in folder2
                        List<String[]> reportData = new ArrayList<>();
                        reportData.add(new String[]{file1Path.getFileName().toString(), "No", "Missing in folder2", "", ""});

                        // Create output directory with timestamp
                        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                        String fileBaseName = file1Path.getFileName().toString().replace(".", "_");
                        Path outputDir = Paths.get(baseOutputPath, fileBaseName + "_" + timeStamp);
                        Files.createDirectories(outputDir);

                        // Generate Excel report
                        String excelFilePath = outputDir.resolve(fileBaseName + ".xlsx").toString();
                        ReportUtils.generateExcelReport(excelFilePath, reportData);
                        System.out.println("Excel report generated successfully at: " + excelFilePath);

                        // Generate HTML report
                        List<String[]> htmlReportData = new ArrayList<>();
                        htmlReportData.add(new String[]{file1Path.getFileName().toString(), "No", "Missing in folder2", "", ""});
                        String htmlFilePath = outputDir.resolve(fileBaseName + ".html").toString();
                        ReportUtils.generateHTMLReport(htmlFilePath, htmlReportData);
                        System.out.println("HTML report generated successfully at: " + htmlFilePath);
                    }
                } catch (IOException | CsvValidationException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
