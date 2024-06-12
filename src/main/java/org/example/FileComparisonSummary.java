package org.example;

public class FileComparisonSummary {
    private String fileName;
    private int matchedColumns;
    private int unmatchedColumns;

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
