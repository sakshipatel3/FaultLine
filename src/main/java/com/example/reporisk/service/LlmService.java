package com.example.reporisk.service;

import com.example.reporisk.model.FileRisk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
public class LlmService {

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;

    public LlmService(@Value("${llm.api.url:}") String apiUrl,
                      @Value("${llm.api.key:}") String apiKey) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.webClient = WebClient.create();
    }

    public boolean isEnabled() {
        return !apiUrl.isBlank() && !apiKey.isBlank();
    }

    public List<String> generateInsights(List<FileRisk> topFiles) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        String prompt = buildPrompt(topFiles);

        try {
            String response = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("prompt", prompt))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null || response.isBlank()) {
                return Collections.emptyList();
            }

            return Arrays.stream(response.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String buildPrompt(List<FileRisk> topFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert Java engineer. Given the following file risk summaries, produce concise remediation suggestions (2-5 bullets per file). Return only newline-separated bullets.\n\n");
        for (FileRisk f : topFiles) {
            sb.append(String.format("%s | risk=%.3f | mods=%d | contributors=%d | churn=%d | ast=%.3f\n",
                    f.getPath(), f.getRiskScore(), f.getModifications(), f.getContributors(), f.getChurnLines(), f.getAstSizeScore()));
        }
        sb.append("\nSuggestions:\n");
        return sb.toString();
    }
}
