package com.example.reporisk.model;

import java.util.List;

public class AnalysisResponse {

    private String repositoryPath;
    private int commitsAnalyzed;
    private List<FileRisk> fileRisks;
    private List<String> insights;

    public AnalysisResponse() {
    }

    public AnalysisResponse(String repositoryPath, int commitsAnalyzed, List<FileRisk> fileRisks, List<String> insights) {
        this.repositoryPath = repositoryPath;
        this.commitsAnalyzed = commitsAnalyzed;
        this.fileRisks = fileRisks;
        this.insights = insights;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(String repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public int getCommitsAnalyzed() {
        return commitsAnalyzed;
    }

    public void setCommitsAnalyzed(int commitsAnalyzed) {
        this.commitsAnalyzed = commitsAnalyzed;
    }

    public List<FileRisk> getFileRisks() {
        return fileRisks;
    }

    public void setFileRisks(List<FileRisk> fileRisks) {
        this.fileRisks = fileRisks;
    }

    public List<String> getInsights() {
        return insights;
    }

    public void setInsights(List<String> insights) {
        this.insights = insights;
    }
}
