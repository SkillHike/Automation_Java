package org.example;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DynamicReportGenerator {

    public static void generateReports(String folder1, String folder2, String baseOutputPath) {
        List<FileComparisonSummary> summaryList = new ArrayList<>();

        try {
            Files.list(Paths.get(folder1)).forEach(filePath1 -> {
                String fileName = filePath1.getFileName().toString();
                try {
                    List<List<String>> file1Data = readFile(filePath1.toString());
                    List<List<String>> file2Data = readFile(Paths.get(folder2, fileName).toString());

                    System.out.println("Comparison started for " + fileName);

                    List<String[]> comparisonResult = FileComparisonUtils.compareFiles(file1Data, file2Data);

                    System.out.println("Comparison completed for " + fileName);

                    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String outputDir = Paths.get(baseOutputPath, fileName + "_" + timestamp).toString();
                    Files.createDirectories(Paths.get(outputDir));

                    String htmlReportPath = Paths.get(outputDir, "report.html").toString();
                    String excelReportPath = Paths.get(outputDir, "report.xlsx").toString();

                    ReportUtils.generateHTMLReport(htmlReportPath, comparisonResult);
                    ReportUtils.generateExcelReport(excelReportPath, comparisonResult);

                    // Generate individual Extent Report for each file comparison
                    String extentReportPath = Paths.get(outputDir, "ExtentReport.html").toString();
                    FileComparisonSummary summary = generateExtentReport(fileName, comparisonResult, extentReportPath, outputDir);
                    summaryList.add(summary);

                } catch (IOException | CsvValidationException e) {
                    e.printStackTrace();
                }
            });

            // Generate final consolidated Extent Report
            String consolidatedReportPath = Paths.get(baseOutputPath, "Consolidated_ExtentReport.html").toString();
            generateConsolidatedReport(summaryList, consolidatedReportPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static FileComparisonSummary generateExtentReport(String fileName, List<String[]> comparisonResult, String reportPath, String outputDir) throws IOException {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setDocumentTitle("File Comparison Report - " + fileName);
        sparkReporter.config().setReportName("File Comparison Report - " + fileName);

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        ExtentTest logger = extent.createTest("File Comparison Test - " + fileName);

        int totalMatched = 0;
        int totalUnmatched = 0;

        for (int i = 0; i < comparisonResult.size(); i += 4) {
            String[] tradeIdRow = comparisonResult.get(i);
            String tradeId = tradeIdRow[0];
            String[] dataInEnv1 = comparisonResult.get(i + 1);
            String[] dataInEnv2 = comparisonResult.get(i + 2);
            String[] differences = comparisonResult.get(i + 3);

            int matchedColumns = 0;
            int unmatchedColumns = 0;
            for (int j = 1; j < differences.length; j++) {
                if ("matched".equals(differences[j])) {
                    matchedColumns++;
                    totalMatched++;
                } else {
                    unmatchedColumns++;
                    totalUnmatched++;
                }
            }

            logger.info("Trade ID: " + tradeId)
                    .info("Matched Columns: " + matchedColumns)
                    .info("Unmatched Columns: " + unmatchedColumns);
        }

        logger.info("Total Matched Columns: " + totalMatched)
                .info("Total Unmatched Columns: " + totalUnmatched);

        // Generate chart
        String chartPath = Paths.get(outputDir, "comparison_chart.png").toString();
        generateComparisonChart(totalMatched, totalUnmatched, chartPath);

        // Embed chart in report
        logger.addScreenCaptureFromPath(chartPath);

        extent.flush();

        return new FileComparisonSummary(fileName, totalMatched, totalUnmatched);
    }

    private static void generateComparisonChart(int matched, int unmatched, String chartPath) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(matched, "Matched", "Columns");
        dataset.addValue(unmatched, "Unmatched", "Columns");

        JFreeChart barChart = ChartFactory.createBarChart(
                "Comparison Results",
                "Category",
                "Count",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        ChartUtils.saveChartAsPNG(new File(chartPath), barChart, 800, 600);
    }

    private static void generateConsolidatedReport(List<FileComparisonSummary> summaryList, String reportPath) throws IOException {
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setDocumentTitle("Consolidated Comparison Report");
        sparkReporter.config().setReportName("Consolidated Comparison Report");

        ExtentReports extent = new ExtentReports();
        extent.attachReporter(sparkReporter);

        ExtentTest summaryTest = extent.createTest("Summary Report");

        int overallMatched = 0;
        int overallUnmatched = 0;

        for (FileComparisonSummary summary : summaryList) {
            summaryTest.info("File: " + summary.getFileName())
                    .info("Matched Columns: " + summary.getMatchedColumns())
                    .info("Unmatched Columns: " + summary.getUnmatchedColumns());

            overallMatched += summary.getMatchedColumns();
            overallUnmatched += summary.getUnmatchedColumns();
        }

        summaryTest.info("Total Files Processed: " + summaryList.size())
                .info("Overall Matched Columns: " + overallMatched)
                .info("Overall Unmatched Columns: " + overallUnmatched);

        // Generate overall chart
        String overallChartPath = Paths.get(reportPath).getParent().resolve("overall_comparison_chart.png").toString();
        generateComparisonChart(overallMatched, overallUnmatched, overallChartPath);

        // Embed overall chart in report
        summaryTest.addScreenCaptureFromPath(overallChartPath);

        extent.flush();
    }

    private static List<List<String>> readFile(String filePath) throws IOException, CsvValidationException {
        if (filePath.endsWith(".csv")) {
            return FileComparisonUtils.readCSV(filePath);
        } else if (filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
            try {
                if (isValidExcelFile(filePath)) {
                    return FileComparisonUtils.readExcel(filePath);
                } else {
                    throw new IllegalArgumentException("File is not a valid Excel file: " + filePath);
                }
            } catch (NotOfficeXmlFileException e) {
                throw new IllegalArgumentException("File is not a valid Excel file: " + filePath, e);
            }
        } else if (filePath.endsWith(".txt")) {
            return FileComparisonUtils.readTextFile(filePath);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + filePath);
        }
    }

    private static boolean isValidExcelFile(String filePath) throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
            OPCPackage.open(is).close();
            return true;
        } catch (InvalidFormatException | NotOfficeXmlFileException e) {
            return false;
        }
    }
}

class FileComparisonSummary {
    private final String fileName;
    private final int matchedColumns;
    private final int unmatchedColumns;

    public FileComparisonSummary(String fileName, int matchedColumns, int unmatchedColumns) {
        this.fileName = fileName;
        this.matchedColumns = matchedColumns;
        this.unmatchedColumns = unmatchedColumns;
    }

    public String getFileName() {
        return fileName;
    }

    public int getMatchedColumns() {
        return matchedColumns;
    }

    public int getUnmatchedColumns() {
        return unmatchedColumns;
    }
}

