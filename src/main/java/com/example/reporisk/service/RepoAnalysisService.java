package com.example.reporisk.service;

import com.example.reporisk.model.AnalysisRequest;
import com.example.reporisk.model.AnalysisResponse;
import com.example.reporisk.model.FileRisk;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that analyzes a local Git repository using JGit and Eclipse JDT.
 * - Traverses commits up to maxCommits
 * - Collects file modification counts, churn lines, contributor counts
 * - Parses Java files with JDT AST to extract simple structural metrics
 * - Computes a combined risk score per file (heuristic)
 */
@Service
public class RepoAnalysisService {

    /**
     * Analyze repository at the given path.
     *
     * @param request request with repoPath and maxCommits
     * @return aggregated analysis
     */
    public AnalysisResponse analyzeRepository(AnalysisRequest request) throws Exception {
        validateRequest(request);

        Path repoPath = Paths.get(request.getRepoPath()).toAbsolutePath().normalize();
        if (!Files.exists(repoPath)) {
            throw new IllegalArgumentException("Repository path does not exist: " + repoPath);
        }

        // Try to open a Git repository at the path
        try (Repository repository = openRepository(repoPath)) {
            if (repository == null) {
                throw new IllegalArgumentException("No git repository found at: " + repoPath);
            }

            Map<String, Integer> fileModificationCounts = new HashMap<>();
            Map<String, Integer> fileChurnLines = new HashMap<>();
            Map<String, Set<String>> fileContributors = new HashMap<>();

            int commitsAnalyzed = traverseCommits(repository, request.getMaxCommits(),
                    fileModificationCounts, fileChurnLines, fileContributors);

            // For each file, if Java file, compute AST metrics
            Map<String, Double> fileAstScores = new HashMap<>();
            for (String filePath : fileModificationCounts.keySet()) {
                if (filePath.endsWith(".java")) {
                    Path absoluteFile = repoPath.resolve(filePath);
                    // File may not exist in working tree if it was deleted in history; try to find current file
                    if (!Files.exists(absoluteFile)) {
                        // skip if not present in working copy
                        continue;
                    }
                    try {
                        String source = Files.readString(absoluteFile, StandardCharsets.UTF_8);
                        double astScore = computeAstSizeScore(source);
                        fileAstScores.put(filePath, astScore);
                    } catch (IOException e) {
                        // If file can't be read, skip AST scoring for it
                        e.printStackTrace();
                    }
                }
            }

            // Compose FileRisk entries
            List<FileRisk> fileRisks = new ArrayList<>();
            for (String filePath : fileModificationCounts.keySet()) {
                int mods = fileModificationCounts.getOrDefault(filePath, 0);
                int churn = fileChurnLines.getOrDefault(filePath, 0);
                int contributors = fileContributors.getOrDefault(filePath, Collections.emptySet()).size();
                double astScore = fileAstScores.getOrDefault(filePath, 0.0);

                double risk = computeCombinedRisk(mods, contributors, churn, astScore);
                FileRisk fr = new FileRisk(filePath, mods, contributors, churn, astScore, round(risk, 3));
                fileRisks.add(fr);
            }

            // Sort by risk descending
            fileRisks.sort(Comparator.comparingDouble(FileRisk::getRiskScore).reversed());

            // Generate simple AI-like insights (heuristic rules)
            List<String> insights = generateInsights(fileRisks);

            AnalysisResponse response = new AnalysisResponse(repoPath.toString(), commitsAnalyzed, fileRisks, insights);
            return response;
        }
    }

    private void validateRequest(AnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        if (request.getRepoPath() == null || request.getRepoPath().trim().isEmpty()) {
            throw new IllegalArgumentException("repoPath is required");
        }
        if (request.getMaxCommits() <= 0) {
            throw new IllegalArgumentException("maxCommits must be > 0");
        }
    }

    private Repository openRepository(Path repoPath) throws IOException {
        // Try as a bare repo or normal git folder
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.setWorkTree(repoPath.toFile());
        builder.findGitDir(repoPath.toFile());
        Repository repository;
        try {
            repository = builder.build();
            if (!repository.getObjectDatabase().exists()) {
                repository.close();
                return null;
            }
            return repository;
        } catch (IOException e) {
            // try alternative: repoPath/.git
            Path gitDir = repoPath.resolve(".git");
            if (Files.exists(gitDir)) {
                builder.setGitDir(gitDir.toFile());
                repository = builder.build();
                return repository;
            }
            throw e;
        }
    }

    private int traverseCommits(Repository repository, int maxCommits,
                                Map<String, Integer> fileModificationCounts,
                                Map<String, Integer> fileChurnLines,
                                Map<String, Set<String>> fileContributors) throws Exception {

        try (RevWalk walk = new RevWalk(repository)) {
            ObjectId head = repository.resolve("HEAD");
            if (head == null) {
                return 0;
            }
            RevCommit start = walk.parseCommit(head);
            walk.markStart(start);

            int count = 0;
            for (RevCommit commit : walk) {
                if (count >= maxCommits) break;
                if (commit.getParentCount() == 0) {
                    // initial commit: diff against empty tree
                    analyzeDiff(repository, null, commit, fileModificationCounts, fileChurnLines, fileContributors);
                } else {
                    for (RevCommit parent : commit.getParents()) {
                        analyzeDiff(repository, parent, commit, fileModificationCounts, fileChurnLines, fileContributors);
                    }
                }
                // record contributor for touched files
                String author = commit.getAuthorIdent() != null ? commit.getAuthorIdent().getEmailAddress() : commit.getAuthorIdent().getName();
                if (author == null) author = "unknown";
                // Contributors tracked inside analyzeDiff

                count++;
            }
            walk.dispose();
            return count;
        }
    }

    private void analyzeDiff(Repository repository, RevCommit parent, RevCommit commit,
                             Map<String, Integer> fileModificationCounts,
                             Map<String, Integer> fileChurnLines,
                             Map<String, Set<String>> fileContributors) throws IOException {

        try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repository);
            df.setDetectRenames(true);

            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            if (parent != null) {
                try (ObjectReader reader = repository.newObjectReader()) {
                    oldTreeIter.reset(reader, parent.getTree());
                    newTreeIter.reset(reader, commit.getTree());
                }
            } else {
                // initial commit: empty old tree
                try (ObjectReader reader = repository.newObjectReader()) {
                    newTreeIter.reset(reader, commit.getTree());
                }
            }

            List<DiffEntry> entries;
            if (parent != null) {
                entries = df.scan(parent.getTree(), commit.getTree());
            } else {
                entries = df.scan(new EmptyTreeIterator(), newTreeIter);
            }

            for (DiffEntry entry : entries) {
                String path = entry.getNewPath();
                if (path == null || path.equals(DiffEntry.DEV_NULL)) {
                    path = entry.getOldPath();
                }
                if (path == null || path.equals(DiffEntry.DEV_NULL)) continue;

                // increment modification count
                fileModificationCounts.put(path, fileModificationCounts.getOrDefault(path, 0) + 1);

                // attempt to compute churn (lines added + removed) from the edit list
                int churn = computeChurnForDiffEntry(df, entry);
                fileChurnLines.put(path, fileChurnLines.getOrDefault(path, 0) + churn);

                // contributor set
                String author = commit.getAuthorIdent() != null ? commit.getAuthorIdent().getEmailAddress() : commit.getAuthorIdent().getName();
                if (author == null) author = "unknown";
                fileContributors.computeIfAbsent(path, k -> new HashSet<>()).add(author);
            }
        } catch (Exception e) {
            // Non-fatal — continue analysis for other commits
            e.printStackTrace();
        }
    }

    private int computeChurnForDiffEntry(DiffFormatter df, DiffEntry entry) {
        try {
            // produce edit list for entry
            FileHeader fh = df.toFileHeader(entry);
            int churn = 0;
            for (Edit edit : fh.toEditList()) {
                churn += Math.abs(edit.getEndA() - edit.getBeginA());
                churn += Math.abs(edit.getEndB() - edit.getBeginB());
            }
            return churn;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Compute a simple AST-based "size" score:
     * - larger classes, longer methods, and methods with many parameters increase score.
     * - score normalized to [0, 10] range heuristically.
     */
    private double computeAstSizeScore(String source) {
        try {
            ASTParser parser = ASTParser.newParser(AST.JLS8);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setSource(source.toCharArray());
            parser.setBindingsRecovery(true);
            parser.setResolveBindings(false);

            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            AstMetricsVisitor visitor = new AstMetricsVisitor();
            cu.accept(visitor);

            // Heuristic combination
            double classScore = Math.log1p(visitor.getTotalClassLines()) / 5.0;
            double methodCountScore = Math.log1p(visitor.getMethodCount()) / 3.0;
            double longMethodScore = Math.log1p(visitor.getLongMethodCount()) / 2.0;
            double paramScore = Math.log1p(visitor.getTotalParameters()) / 4.0;

            double raw = classScore + methodCountScore + longMethodScore + paramScore;
            // map to 0-10
            double mapped = Math.min(10.0, raw * 2.0);
            return round(mapped, 3);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Compute a combined risk score using heuristic weights:
     * - modifications: more modifications -> higher risk
     * - contributors: more contributors -> higher risk (coordination)
     * - churn lines: more churn -> higher risk
     * - astScore: larger/more complex code -> higher risk
     *
     * Final score normalized to 0..10
     */
    private double computeCombinedRisk(int modifications, int contributors, int churnLines, double astScore) {
        double wMods = 0.35;
        double wContrib = 0.2;
        double wChurn = 0.25;
        double wAst = 0.2;

        // Normalize inputs heuristically
        double normMods = Math.log1p(modifications) / Math.log1p(50); // assumes 50 modifications is big
        double normContrib = Math.log1p(contributors) / Math.log1p(20);
        double normChurn = Math.log1p(churnLines) / Math.log1p(1000); // 1000 churn lines big
        double normAst = astScore / 10.0;

        double score = (wMods * normMods + wContrib * normContrib + wChurn * normChurn + wAst * normAst) * 10.0;
        // Clamp
        score = Math.max(0.0, Math.min(10.0, score));
        return score;
    }

    private List<String> generateInsights(List<FileRisk> fileRisks) {
        List<String> insights = new ArrayList<>();
        if (fileRisks == null || fileRisks.isEmpty()) {
            insights.add("No files analyzed.");
            return insights;
        }

        // Top risky files
        List<FileRisk> top = fileRisks.stream().limit(5).collect(Collectors.toList());
        insights.add("Top risky files:");
        for (FileRisk fr : top) {
            insights.add(String.format("  - %s (risk=%.3f, mods=%d, contributors=%d, churn=%d, ast=%.3f)",
                    fr.getPath(), fr.getRiskScore(), fr.getModifications(), fr.getContributors(), fr.getChurnLines(), fr.getAstSizeScore()));
        }

        // Detect hotspots with high churn and many contributors
        List<FileRisk> hotspots = fileRisks.stream()
                .filter(f -> f.getChurnLines() > 200 && f.getContributors() > 2)
                .collect(Collectors.toList());
        if (!hotspots.isEmpty()) {
            insights.add("Hotspots with sustained churn and multiple contributors detected:");
            for (FileRisk fr : hotspots) {
                insights.add(String.format("  - %s needs refactor consideration (churn=%d, contributors=%d)", fr.getPath(), fr.getChurnLines(), fr.getContributors()));
            }
        } else {
            insights.add("No high-churn hotspots with many contributors were detected.");
        }

        // Suggest files with large AST score
        List<FileRisk> largeAst = fileRisks.stream()
                .filter(f -> f.getAstSizeScore() >= 6.0)
                .collect(Collectors.toList());
        if (!largeAst.isEmpty()) {
            insights.add("Files with large/complex code structures (consider decomposition):");
            for (FileRisk fr : largeAst) {
                insights.add(String.format("  - %s (astScore=%.3f)", fr.getPath(), fr.getAstSizeScore()));
            }
        }

        // Simple recommendation
        insights.add("Recommendations:");
        insights.add("  - Prioritize top risky files for code review and targeted testing.");
        insights.add("  - Consider refactoring files with high AST complexity and high churn.");
        insights.add("  - Investigate files with many contributors to improve ownership or documentation.");

        return insights;
    }

    private double round(double v, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(v * factor) / factor;
    }

    /**
     * Visitor to collect simple AST metrics.
     */
    private static class AstMetricsVisitor extends ASTVisitor {
        private int totalClassLines = 0;
        private int methodCount = 0;
        private int longMethodCount = 0;
        private int totalParameters = 0;

        @Override
        public boolean visit(TypeDeclaration node) {
            int start = node.getStartPosition();
            int length = node.getLength();
            int lines = estimateLines(node);
            totalClassLines += Math.max(0, lines);
            return super.visit(node);
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            methodCount++;
            int lines = estimateLines(node);
            if (lines > 50) longMethodCount++;
            totalParameters += (node.parameters() != null ? node.parameters().size() : 0);
            return super.visit(node);
        }

        private int estimateLines(ASTNode node) {
            // Approximate lines by counting newlines in the source segment if available
            try {
                // ASTNode doesn't expose source directly here; use length as heuristic
                int length = node.getLength();
                int approx = Math.max(1, length / 60); // crude approximation
                return approx;
            } catch (Exception e) {
                return 1;
            }
        }

        public int getTotalClassLines() {
            return totalClassLines;
        }

        public int getMethodCount() {
            return methodCount;
        }

        public int getLongMethodCount() {
            return longMethodCount;
        }

        public int getTotalParameters() {
            return totalParameters;
        }
    }
}
