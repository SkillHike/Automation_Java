import com.opencsv.exceptions.CsvValidationException;
import org.apache.poi.ss.usermodel.*;
import org.testng.annotations.Test;
import org.example.DynamicReportGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class FileComparisonTest {

    @Test
    public void testCompareFilesInFolders() throws IOException, CsvValidationException {
        // Load properties from the config file
        Properties props = new Properties();
        FileInputStream configInput = new FileInputStream("C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\config.properties");
        props.load(configInput);
        configInput.close();

        // Load Excel file
        FileInputStream excelFile = new FileInputStream(new File("C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\DataSheet.xlsx"));
        Workbook workbook = WorkbookFactory.create(excelFile);
        Sheet sheet = workbook.getSheetAt(0);

        // Iterate through rows and columns
        for (Row row : sheet) {
            Cell comparisonCell = row.getCell(4); // Comparision column
            if (comparisonCell != null && "yes".equalsIgnoreCase(comparisonCell.getStringCellValue())) {
                String path = row.getCell(3).getStringCellValue(); // Path column
                String folderNameEnv1 = props.getProperty("folder1");
                String folderNameEnv2 = props.getProperty("folder2");
                String reportName = row.getCell(2).getStringCellValue(); // Reportname column

                // Construct the paths
                String folder1 = path + "\\" + folderNameEnv1;
                String folder2 = path + "\\" + folderNameEnv2;
                String baseOutputPath = "C:\\Users\\manju\\IdeaProjects\\filecomparision\\target";

                // Perform the file comparison
                DynamicReportGenerator.generateReports(folder1, folder2, baseOutputPath, reportName);
            }
        }

        workbook.close();
        excelFile.close();
    }
}
