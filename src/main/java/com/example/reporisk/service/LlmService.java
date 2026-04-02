package com.example.reporisk.service;

import com.example.reporisk.model.FileRisk;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
    private static final int MAX_FILE_CONTENT_FOR_LLM = 8_000;

    public static final class CodeSnippet {
        private final String description;
        private final String current;
        private final String suggested;

        public CodeSnippet(String description, String current, String suggested) {
            this.description = description != null ? description : "";
            this.current = current != null ? current : "";
            this.suggested = suggested != null ? suggested : "";
        }
        public String getDescription() { return description; }
        public String getCurrent() { return current; }
        public String getSuggested() { return suggested; }
    }

    public static final class FixStepsResult {
        private final List<String> steps;
        private final List<CodeSnippet> snippets;

        public FixStepsResult(List<String> steps, List<CodeSnippet> snippets) {
            this.steps = steps != null ? steps : Collections.emptyList();
            this.snippets = snippets != null ? snippets : Collections.emptyList();
        }
        public List<String> getSteps() { return steps; }
        public List<CodeSnippet> getSnippets() { return snippets; }
    }

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String model;
    /** openai = OpenAI-compatible chat; huggingface = HF Inference API (transformer text-generation). */
    private final String provider;
    private final ObjectMapper mapper = new ObjectMapper();

    public LlmService(@Value("${llm.api.url:}") String apiUrl,
                      @Value("${llm.api.key:}") String apiKey,
                      @Value("${llm.model:gpt-3.5-turbo}") String model,
                      @Value("${llm.provider:openai}") String provider) {
        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "gpt-3.5-turbo" : model.trim();
        this.provider = provider == null ? "openai" : provider.trim();
        this.webClient = WebClient.builder().build();
    }

    private boolean useHuggingface() {
        String p = provider.toLowerCase(Locale.ROOT);
        return "huggingface".equals(p) || "hf".equals(p) || "transformers".equals(p);
    }

    private Map<String, Object> buildOpenAiBody(String system, String user, double temperature) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        List<Map<String, String>> messages = new ArrayList<>();
        if (system != null && !system.isBlank()) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of("role", "user", "content", user == null ? "" : user));
        body.put("messages", messages);
        body.put("temperature", temperature);
        return body;
    }

    private Map<String, Object> buildHuggingfaceBody(String system, String user, double temperature, int maxNewTokens) {
        String combined = hfCombinedPrompt(system, user);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_new_tokens", maxNewTokens);
        parameters.put("temperature", temperature);
        parameters.put("return_full_text", false);
        Map<String, Object> body = new HashMap<>();
        body.put("inputs", combined);
        body.put("parameters", parameters);
        return body;
    }

    private static String hfCombinedPrompt(String system, String user) {
        String s = system == null ? "" : system.trim();
        String u = user == null ? "" : user.trim();
        if (s.isEmpty()) return u;
        if (u.isEmpty()) return s;
        return s + "\n\n" + u;
    }

    private String postLlm(Object body, Duration timeout) {
        return webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        res -> res.bodyToMono(String.class)
                                .map(b -> (Throwable) new RuntimeException(extractErrorMessage(b))))
                .bodyToMono(String.class)
                .timeout(timeout)
                .block();
    }

    /**
     * Single completion: OpenAI-style chat or Hugging Face text-generation, depending on {@code llm.provider}.
     */
    private String completeChat(String systemPrompt, String userPrompt, double temperature,
                                Duration timeout, int huggingFaceMaxNewTokens) throws Exception {
        String respBody;
        if (useHuggingface()) {
            respBody = postLlm(buildHuggingfaceBody(systemPrompt, userPrompt, temperature, huggingFaceMaxNewTokens), timeout);
            return parseHuggingfaceAssistantText(respBody);
        }
        respBody = postLlm(buildOpenAiBody(systemPrompt, userPrompt, temperature), timeout);
        return extractOpenAiAssistantText(respBody);
    }

    private String extractOpenAiAssistantText(String respBody) throws Exception {
        if (respBody == null || respBody.isBlank()) return "";
        JsonNode root = mapper.readTree(respBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.size() == 0) return "";
        JsonNode message = choices.get(0).path("message");
        String content = extractMessageContent(message, choices.get(0));
        return content != null ? content.trim() : "";
    }

    private String parseHuggingfaceAssistantText(String respBody) throws Exception {
        if (respBody == null || respBody.isBlank()) return "";
        JsonNode root = mapper.readTree(respBody);
        if (root.has("error")) {
            JsonNode err = root.get("error");
            String msg = err.isTextual() ? err.asText() : err.toString();
            throw new RuntimeException(msg);
        }
        String text = extractHfGeneratedText(root);
        return text != null ? text.trim() : "";
    }

    private String extractHfGeneratedText(JsonNode node) {
        if (node == null || node.isNull()) return "";
        if (node.isArray()) {
            if (node.size() == 0) return "";
            return extractHfGeneratedText(node.get(0));
        }
        if (node.isObject() && node.has("generated_text")) {
            return node.path("generated_text").asText("");
        }
        return "";
    }

    public boolean isEnabled() {
        return !apiUrl.isBlank() && !apiKey.isBlank();
    }

    /**
     * Call the LLM with a minimal prompt to verify URL + key work. Returns 200 with message or 502 with error.
     */
    public ResponseEntity<?> testConnection() {
        try {
            String content = completeChat("", "Reply with exactly: OK", 0, Duration.ofSeconds(15), 32);
            if (content.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("ok", false, "message", "Empty response from LLM"));
            }
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
        String system = "You are an expert Java engineer who explains things in simple, clear language. " +
                "For each file you see, summarize why it is risky and give 2-4 short, concrete suggestions. " +
                "Avoid jargon; write as if to a competent developer who is new to the codebase.";

        try {
            String content = completeChat(system, prompt, 0.2, Duration.ofSeconds(20), 2048);
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

        String system = "You are a helpful coding assistant. Reply only with numbered steps to fix the given file. Use simple, clear language. No markdown, no extra text.";

        log.info("Calling LLM for fix steps (provider={}, model={}, file={})", provider, model, file.getPath());
        try {
            String content = completeChat(system, prompt, 0.3, Duration.ofSeconds(30), 1024);
            if (content.isBlank()) {
                log.debug("LLM response: empty assistant text");
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

    /**
     * Generate fix steps and optional code snippets. When file content is provided, steps come from
     * the existing flow and snippets from a dedicated second LLM call (simpler prompt = better JSON).
     */
    public FixStepsResult generateFixStepsWithSnippets(FileRisk file, String repositoryPath, String fileContent) {
        List<String> steps = generateFixSteps(file, repositoryPath);
        if (fileContent == null || fileContent.isBlank()) {
            return new FixStepsResult(steps, Collections.emptyList());
        }
        List<CodeSnippet> snippets = fetchSnippetsOnly(file.getPath(), fileContent);
        return new FixStepsResult(steps, snippets);
    }

    /**
     * Single-purpose LLM call: return a JSON array of { description, current, suggested } for refactoring the given code.
     */
    private List<CodeSnippet> fetchSnippetsOnly(String filePath, String fileContent) {
        String truncated = fileContent.length() > MAX_FILE_CONTENT_FOR_LLM
                ? fileContent.substring(0, MAX_FILE_CONTENT_FOR_LLM) + "\n\n... (truncated)"
                : fileContent;
        String userPrompt = String.format(
                "File: %s\n\nJava code:\n```java\n%s\n```\n\n"
                + "Suggest 1 to 3 small refactors. Reply with ONLY a JSON array, no other text. Each element: {\"description\": \"what to change\", \"current\": \"exact code to replace\", \"suggested\": \"replacement code\"}. Keep each current/suggested under 15 lines.",
                filePath, truncated
        );
        String system = "You output only a JSON array. No markdown, no code fence, no explanation. Array of objects with keys: description, current, suggested.";

        try {
            String content = completeChat(system, userPrompt, 0.2, Duration.ofSeconds(30), 2048);
            if (content.isBlank()) return Collections.emptyList();
            String cleaned = content.trim().replaceAll("^```(?:json)?\\s*", "").replaceAll("\\s*```$", "").trim();
            if (cleaned.isEmpty()) return Collections.emptyList();

            JsonNode arr = mapper.readTree(cleaned);
            if (!arr.isArray()) {
                JsonNode snippetsNode = arr.path("snippets");
                if (snippetsNode.isArray()) arr = snippetsNode;
                else return Collections.emptyList();
            }
            List<CodeSnippet> list = new ArrayList<>();
            for (JsonNode obj : arr) {
                String desc = obj.path("description").asText("");
                String cur = obj.path("current").asText("");
                String sug = obj.path("suggested").asText("");
                if (!cur.isEmpty() || !sug.isEmpty()) {
                    list.add(new CodeSnippet(desc, cur, sug));
                }
            }
            log.info("Fetched {} code snippet(s) for {}", list.size(), filePath);
            return list;
        } catch (Exception e) {
            log.warn("Snippets LLM call failed (steps still returned): {}", e.getMessage());
            return Collections.emptyList();
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
