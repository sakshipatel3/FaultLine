package com.example.reporisk.controller;

import com.example.reporisk.model.AnalysisRequest;
import com.example.reporisk.model.AnalysisResponse;
import com.example.reporisk.model.FileRisk;
import com.example.reporisk.service.LlmService;
import com.example.reporisk.service.RepoAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "false")
public class RepoAnalysisController {

    private final RepoAnalysisService service;
    private final LlmService llmService;

    public RepoAnalysisController(RepoAnalysisService service, LlmService llmService) {
        this.service = service;
        this.llmService = llmService;
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

    /**
     * Test that the LLM is reachable and the API key works.
     * Returns 200 with a short message if OK, or 502 with error details.
     */
    @GetMapping("/health/llm")
    public ResponseEntity<?> healthLlm() {
        if (!llmService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("ok", false, "message", "LLM not configured. Set LLM_API_URL and LLM_API_KEY in .env"));
        }
        return llmService.testConnection();
    }

    /**
     * Get AI-generated fix steps for a single file (from the analysis result).
     * Body: { "repositoryPath": "...", "file": { "path", "riskScore", "modifications", "contributors", "churnLines", "astSizeScore" } }
     * Returns: { "steps": ["1. ...", "2. ..."] } or 503 if LLM is not configured.
     */
    @PostMapping("/insights/file")
    public ResponseEntity<?> getFixStepsForFile(@RequestBody Map<String, Object> body) {
        if (!llmService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("AI insights are not configured. Set LLM_API_URL and LLM_API_KEY.");
        }
        String repositoryPath = body.get("repositoryPath") != null ? body.get("repositoryPath").toString() : "";
        @SuppressWarnings("unchecked")
        Map<String, Object> fileMap = (Map<String, Object>) body.get("file");
        if (fileMap == null) {
            return ResponseEntity.badRequest().body("Missing 'file' in request body.");
        }
        FileRisk file = new FileRisk();
        file.setPath(getString(fileMap, "path"));
        file.setRiskScore(getDouble(fileMap, "riskScore"));
        file.setModifications(getInt(fileMap, "modifications"));
        file.setContributors(getInt(fileMap, "contributors"));
        file.setChurnLines(getInt(fileMap, "churnLines"));
        file.setAstSizeScore(getDouble(fileMap, "astSizeScore"));
        try {
            List<String> steps = llmService.generateFixSteps(file, repositoryPath);
            return ResponseEntity.ok(Map.of("steps", steps));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "LLM failed";
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(msg);
        }
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    private static int getInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static double getDouble(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
