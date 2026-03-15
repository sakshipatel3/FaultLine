package com.example.reporisk.controller;

import com.example.reporisk.model.AnalysisRequest;
import com.example.reporisk.model.AnalysisResponse;
import com.example.reporisk.service.RepoAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "false")
public class RepoAnalysisController {

    private final RepoAnalysisService service;

    public RepoAnalysisController(RepoAnalysisService service) {
        this.service = service;
    }

    /**
     * Perform analysis on a local git repository path.
     *
     * Body example:
     * {
     *   "repoPath": "/absolute/path/to/repo",
     *   "maxCommits": 1000
     * }
     *
     * Returns aggregated file risk scores and insights.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeRepository(@Valid @RequestBody AnalysisRequest request) {
        try {
            AnalysisResponse response = service.analyzeRepository(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Log and return generic error
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during analysis: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Repo Risk Analyzer is running");
    }
}
