package com.example.reporisk.service;

import com.example.reporisk.model.FileRisk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private static final Pattern NUMBERED_STEP = Pattern.compile("\\s*\\d+\\.\\s*");

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

    /**
     * Call the LLM with a minimal prompt to verify URL + key work. Returns 200 with message or 502 with error.
     */
    public ResponseEntity<?> testConnection() {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "user", "content", "Reply with exactly: OK")
        ));
        body.put("temperature", 0);
        try {
            String respBody = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            res -> res.bodyToMono(String.class)
                                    .map(b -> (Throwable) new RuntimeException(extractErrorMessage(b))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            if (respBody == null || respBody.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("ok", false, "message", "Empty response from LLM"));
            }
            JsonNode root = mapper.readTree(respBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("ok", false, "message", "No choices in response", "raw", respBody.substring(0, Math.min(300, respBody.length()))));
            }
            JsonNode message = choices.get(0).path("message");
            String content = extractMessageContent(message, choices.get(0));
            if (content == null) content = "";
            log.info("LLM test connection OK: {}", content.trim());
            return ResponseEntity.ok(Map.of("ok", true, "message", "LLM connected", "reply", content.trim()));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("LLM test connection failed: {}", msg);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("ok", false, "message", msg));
        }
    }

    public List<String> generateInsights(List<FileRisk> topFiles) {
        if (!isEnabled() || topFiles == null || topFiles.isEmpty()) {
            return Collections.emptyList();
        }

        String prompt = buildPrompt(topFiles);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are an expert Java engineer who explains things in simple, clear language. " +
                        "For each file you see, summarize why it is risky and give 2-4 short, concrete suggestions. " +
                        "Avoid jargon; write as if to a competent developer who is new to the codebase."));
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

    /**
     * Generate concrete, numbered steps to fix a single file based on its risk metrics.
     * @throws RuntimeException with a clear message if the LLM call fails or returns no steps (so the controller can return 502).
     */
    public List<String> generateFixSteps(FileRisk file, String repositoryPath) {
        if (!isEnabled() || file == null) {
            return Collections.emptyList();
        }

        String prompt = String.format(
                "A file in a Java repository has been flagged as risky. Give 4–6 concrete, numbered steps to fix or reduce the risk. "
                + "Write in simple language, as if a bot is answering. Be specific and actionable.\n\n"
                + "Repository: %s\n"
                + "File: %s\n"
                + "Risk score (0–10): %.2f\n"
                + "Modifications (commit count): %d | Contributors: %d | Churn (lines changed): %d | AST complexity score: %.2f\n\n"
                + "Respond with only the numbered steps, one per line (e.g. \"1. ...\", \"2. ...\"). No preamble.",
                repositoryPath == null ? "(unknown)" : repositoryPath,
                file.getPath(),
                file.getRiskScore(),
                file.getModifications(),
                file.getContributors(),
                file.getChurnLines(),
                file.getAstSizeScore()
        );

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are a helpful coding assistant. Reply only with numbered steps to fix the given file. Use simple, clear language. No markdown, no extra text."));
        messages.add(Map.of("role", "user", "content", prompt));
        body.put("messages", messages);
        body.put("temperature", 0.3);

        log.info("Calling LLM for fix steps (model={}, file={})", model, file.getPath());
        try {
            String respBody = webClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            res -> res.bodyToMono(String.class)
                                    .map(b -> (Throwable) new RuntimeException("LLM API error " + res.statusCode() + ": " + extractErrorMessage(b))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (respBody == null || respBody.isBlank()) {
                throw new RuntimeException("LLM returned an empty response.");
            }

            JsonNode root = mapper.readTree(respBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.size() == 0) {
                log.debug("LLM response: choices missing or empty. Raw (first 500 chars): {}", respBody.length() > 500 ? respBody.substring(0, 500) + "…" : respBody);
                throw new RuntimeException("LLM response had no choices. Check the API response format.");
            }
            JsonNode message = choices.get(0).path("message");
            String content = extractMessageContent(message, choices.get(0));
            if (content == null) content = "";
            content = content.trim();
            if (content.isBlank()) {
                log.debug("LLM response: message content empty. message node: {}", message);
                throw new RuntimeException("LLM returned no text in the response.");
            }
            log.debug("LLM returned {} chars of content.", content.length());

            List<String> steps = Arrays.stream(content.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (steps.isEmpty()) {
                steps = parseNumberedStepsFromBlock(content);
            }
            if (steps.isEmpty()) {
                throw new RuntimeException("LLM returned no steps. Try a different model (e.g. gpt-4o) or check the prompt.");
            }
            return steps;
        } catch (WebClientResponseException e) {
            String msg = extractErrorMessage(e.getResponseBodyAsString());
            log.warn("LLM API error: {}", msg);
            throw new RuntimeException("LLM API error: " + msg, e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            log.warn("LLM call failed", e);
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    private static String extractErrorMessage(String body) {
        if (body == null || body.isBlank()) return "No response body";
        try {
            JsonNode root = new ObjectMapper().readTree(body);
            JsonNode err = root.path("error");
            if (!err.isMissingNode()) {
                String msg = err.path("message").asText(null);
                if (msg != null) return msg;
                return err.toString();
            }
            return body.length() > 200 ? body.substring(0, 200) + "…" : body;
        } catch (Exception ignored) {
            return body.length() > 200 ? body.substring(0, 200) + "…" : body;
        }
    }

    /**
     * Extract text from OpenAI-style message. Handles both:
     * - "content": "string"
     * - "content": [ {"type": "text", "text": "..."} ]  (newer API)
     */
    private String extractMessageContent(JsonNode message, JsonNode choice) {
        JsonNode content = message.path("content");
        if (content.isTextual()) {
            return content.asText("");
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : content) {
                if (part.has("text")) {
                    sb.append(part.path("text").asText(""));
                } else if (part.isTextual()) {
                    sb.append(part.asText(""));
                }
            }
            return sb.toString();
        }
        return choice.path("text").asText("");
    }

    private List<String> parseNumberedStepsFromBlock(String content) {
        String[] parts = NUMBERED_STEP.split(content);
        List<String> steps = new ArrayList<>();
        for (String part : parts) {
            String s = part.trim();
            if (s.length() > 2) steps.add(s);
        }
        return steps;
    }
}
