package com.example.reporisk.service;

import com.example.reporisk.model.FileRisk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LlmService {

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmService(@Value("${llm.api.url:}") String apiUrl,
                      @Value("${llm.api.key:}") String apiKey,
                      @Value("${llm.model:gpt-3.5-turbo}") String model) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "gpt-3.5-turbo" : model.trim();
        this.webClient = WebClient.builder().build();
    }

    public boolean isEnabled() {
        return !apiUrl.isBlank() && !apiKey.isBlank();
    }

    public List<String> generateInsights(List<FileRisk> topFiles) {
        if (!isEnabled() || topFiles == null || topFiles.isEmpty()) {
            return Collections.emptyList();
        }

        String prompt = buildPrompt(topFiles);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "You are an expert Java engineer and technical reviewer. Produce concise, actionable remediation suggestions for each file listed."));
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);
        body.put("temperature", 0.2);

        try {
            Mono<String> respMono = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(20));

            String respBody = respMono.block();
            if (respBody == null || respBody.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode root = mapper.readTree(respBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                return Collections.emptyList();
            }
            JsonNode message = choices.get(0).path("message");
            String content = message.path("content").asText("");
            if (content.isBlank()) {
                content = choices.get(0).path("text").asText("");
            }
            if (content.isBlank()) return Collections.emptyList();

            return Arrays.stream(content.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String buildPrompt(List<FileRisk> topFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("For each file below, provide 2-4 concise bullet remediation suggestions focused on maintainability, refactoring, tests, and ownership. Prefix bullets with the file path.\n\n");
        for (FileRisk f : topFiles) {
            sb.append(String.format("%s | risk=%.3f | mods=%d | contributors=%d | churn=%d | ast=%.3f\n",
                    f.getPath(), f.getRiskScore(), f.getModifications(), f.getContributors(), f.getChurnLines(), f.getAstSizeScore()));
        }
        sb.append("\nSuggestions:\n");
        return sb.toString();
    }
}
