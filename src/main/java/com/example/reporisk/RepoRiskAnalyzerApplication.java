package com.example.reporisk;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class RepoRiskAnalyzerApplication {

    public static void main(String[] args) {
        String cwd = System.getProperty("user.dir");

        // Load .env: try current dir, then parent (e.g. project root when run from target/)
        loadEnvFrom(Paths.get(cwd));
        Path parent = Paths.get(cwd).getParent();
        if (parent != null && Files.isDirectory(parent)) {
            loadEnvFrom(parent);
        }

        boolean llmUrlSet = nonEmpty(System.getProperty("LLM_API_URL"));
        boolean llmKeySet = nonEmpty(System.getProperty("LLM_API_KEY"));
        if (llmUrlSet && llmKeySet) {
            System.out.println("[FaultLine] LLM configured: yes (AI fix steps enabled)");
        } else {
            System.out.println("[FaultLine] LLM configured: no. Edit .env in project root, set LLM_API_KEY=sk-your-key, then restart.");
            System.out.println("[FaultLine] Looked for .env in: " + cwd + (parent != null ? ", " + parent : ""));
        }

        SpringApplication.run(RepoRiskAnalyzerApplication.class, args);
    }

    private static boolean nonEmpty(String s) {
        return s != null && s.trim().length() > 0;
    }

    private static void loadEnvFrom(Path dir) {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .directory(dir.toString())
                    .load();
            applyIfSet("LLM_API_URL", dotenv.get("LLM_API_URL"));
            applyIfSet("LLM_API_KEY", dotenv.get("LLM_API_KEY"));
            applyIfSet("LLM_MODEL", dotenv.get("LLM_MODEL"));
        } catch (Exception ignored) {
        }
    }

    private static void applyIfSet(String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            System.setProperty(key, value.trim());
        }
    }
}
