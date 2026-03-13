package com.example.reporisk.model;

import java.util.Objects;

public class FileRisk {

    private String path;
    private int modifications;
    private int contributors;
    private int churnLines;
    private double astSizeScore;
    private double riskScore;

    public FileRisk() {
    }

    public FileRisk(String path, int modifications, int contributors, int churnLines, double astSizeScore, double riskScore) {
        this.path = path;
        this.modifications = modifications;
        this.contributors = contributors;
        this.churnLines = churnLines;
        this.astSizeScore = astSizeScore;
        this.riskScore = riskScore;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getModifications() {
        return modifications;
    }

    public void setModifications(int modifications) {
        this.modifications = modifications;
    }

    public int getContributors() {
        return contributors;
    }

    public void setContributors(int contributors) {
        this.contributors = contributors;
    }

    public int getChurnLines() {
        return churnLines;
    }

    public void setChurnLines(int churnLines) {
        this.churnLines = churnLines;
    }

    public double getAstSizeScore() {
        return astSizeScore;
    }

    public void setAstSizeScore(double astSizeScore) {
        this.astSizeScore = astSizeScore;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileRisk fileRisk = (FileRisk) o;
        return modifications == fileRisk.modifications && contributors == fileRisk.contributors && churnLines == fileRisk.churnLines && Double.compare(fileRisk.astSizeScore, astSizeScore) == 0 && Double.compare(fileRisk.riskScore, riskScore) == 0 && Objects.equals(path, fileRisk.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, modifications, contributors, churnLines, astSizeScore, riskScore);
    }
}
