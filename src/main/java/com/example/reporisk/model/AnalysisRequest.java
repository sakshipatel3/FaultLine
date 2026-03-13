package com.example.reporisk.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.Objects;

public class AnalysisRequest {

    @NotBlank(message = "repoPath is required and must be a non-empty string")
    private String repoPath;

    @Min(value = 1, message = "maxCommits must be at least 1")
    private int maxCommits = 1000;

    public AnalysisRequest() {
    }

    public AnalysisRequest(String repoPath, int maxCommits) {
        this.repoPath = repoPath;
        this.maxCommits = maxCommits;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public int getMaxCommits() {
        return maxCommits;
    }

    public void setMaxCommits(int maxCommits) {
        this.maxCommits = maxCommits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalysisRequest that = (AnalysisRequest) o;
        return maxCommits == that.maxCommits && Objects.equals(repoPath, that.repoPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoPath, maxCommits);
    }
}
