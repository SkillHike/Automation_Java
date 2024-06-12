import com.opencsv.exceptions.CsvValidationException;
import org.testng.annotations.Test;
import org.example.DynamicReportGenerator;

import java.io.IOException;

public class FileComparisonTest {

    @Test
    public void testCompareFilesInFolders() throws IOException, CsvValidationException {
        String folder1 = "C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\data1";
        String folder2 = "C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\data2";
        String baseOutputPath = "C:\\Users\\manju\\IdeaProjects\\filecomparision\\target";

        DynamicReportGenerator.generateReports(folder1, folder2, baseOutputPath,"C:\\Users\\manju\\IdeaProjects\\filecomparision\\src\\main\\resources\\Reportsheet");
    }
}