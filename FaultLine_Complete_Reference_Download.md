# FaultLine — Complete Reference & Concept Guide

**Private use.** Download this file and store it in a private place (e.g. private GitHub repo or local folder). Use it to explain the project and to answer conceptual questions like “What is Spring Boot?” or “What is generative AI?”

---

# PART A — CONCEPTS (So You Can Answer “What is…?”)

---

## What is Generative AI?

**Generative AI** is artificial intelligence that *generates* new content (text, code, images) instead of only classifying or predicting. It learns patterns from huge amounts of data and then produces new, human-like output when you give it a prompt.

- **In this project:** We use a **large language model (LLM)** as generative AI. We send it a prompt like “Here are risky Java files and their metrics; give 4–6 numbered steps to fix each.” The model *generates* new text (the steps) that didn’t exist before. We don’t store those steps in a database—they are created on the fly by the model.
- **Why use it here:** To turn raw risk numbers into plain-language, actionable advice (e.g. “Split this class into smaller ones”) that a developer can follow. Without it, we’d only show numbers; with it, we get explanations and recommendations.

**Common terms:**
- **LLM (Large Language Model):** A model trained on vast text/code that can complete or generate sentences (e.g. GPT, LLaMA).
- **Prompt:** The text you send to the model (e.g. “Give steps to fix this file”).
- **OpenAI-compatible API:** Many services (OpenAI, Groq, OpenRouter) use the same request format (model, messages, temperature), so we can switch providers by changing URL and key.

---

## What is Spring Boot?

**Spring Boot** is a Java framework that makes it easy to build production-ready web applications and REST APIs. It sits on top of the **Spring Framework** and adds:

- **Auto-configuration:** It sets up sensible defaults (e.g. embedded Tomcat server, JSON serialization) so you write less config.
- **Embedded server:** Your app runs as a single JAR that includes the web server; no need to deploy to a separate Tomcat or Jetty.
- **Dependency injection (DI):** You declare components (e.g. `@Service`, `@RestController`), and Spring creates and wires them. For example, `RepoAnalysisController` receives `RepoAnalysisService` and `LlmService` automatically—you don’t `new` them yourself.
- **Starter dependencies:** One dependency (e.g. `spring-boot-starter-web`) pulls in everything needed for a web app (web, JSON, server).

**In this project:** We use Spring Boot to expose REST endpoints (`/api/analyze`, `/api/health`, `/api/insights/file`), to inject `RepoAnalysisService` and `LlmService` into the controller, and to read configuration (e.g. `llm.api.url`) from properties. The app runs as `java -jar ...jar` with no separate server install.

---

## What is a REST API?

**REST (Representational State Transfer)** is a way to design APIs over HTTP. The server exposes **resources** (e.g. “analysis,” “health”) and clients interact with them using **HTTP methods** (GET, POST, etc.) and **URLs**.

- **GET** — Retrieve something (e.g. `GET /api/health` returns “server is running”).
- **POST** — Send data and get a response (e.g. `POST /api/analyze` with a JSON body `{ "repoPath": "...", "maxCommits": 500 }` returns the analysis result).

**In this project:** The backend is a REST API. The frontend (browser) sends HTTP requests to `http://localhost:8080/api/...`. The backend returns JSON. There are no sessions or cookies for the analysis itself—each request carries all needed data (e.g. repo path, file metrics).

---

## What is React?

**React** is a JavaScript library for building user interfaces. It uses:

- **Components:** Reusable pieces of UI (e.g. a form, a table). In our app, most UI lives in one component, `App`.
- **State:** Data that, when it changes, causes the UI to re-render (e.g. `data` holds the analysis result; when it’s set, the table and report appear).
- **Hooks:** Functions like `useState` to hold state in a component (e.g. `const [data, setData] = useState(null)`).

**In this project:** The frontend is a React app. The user fills a form (repo URL, max commits); on submit we call the backend and store the result in state, then React re-renders and shows the dashboard, table, and insights. Clicking a row triggers another API call for “steps to fix” and updates state again.

---

## What is Git / JGit? What is “churn”?

**Git** is a version-control system. It stores the history of a project as a series of **commits**. Each commit has a snapshot of files, an author, a message, and a link to the previous commit(s). You can see what changed between two commits (a **diff**).

**JGit** is a Java library that implements Git. So we can, from Java code and without running the `git` command:

- **Clone** a repository from a URL into a folder.
- **Walk** the commit history (e.g. from HEAD back in time).
- **Compute diffs** between parent and child commits to see which files changed and how many lines were added/removed.

**Churn** in our project means “how much a file has been changed over time.” We measure it by:

- **Modification count:** How many commits touched that file.
- **Lines changed:** For each commit, we sum the size of each edit (lines added + removed) for that file across all edits in the diff.

High churn often indicates a “hotspot”—a file that many people change often, which can mean higher bug risk or technical debt.

---

## What is an AST (Abstract Syntax Tree)?

An **Abstract Syntax Tree** is a tree representation of source code. Each node is a construct (e.g. a class, a method, a parameter). The parser reads the text and builds this tree so that tools can analyze structure without executing the code.

**In this project:** We use **Eclipse JDT** to parse Java source into an AST. We then walk the tree with a **visitor** that counts:

- Type declarations (classes)
- Method declarations
- Methods longer than a threshold (“long methods”)
- Total parameters

From that we compute an “AST complexity score” (0–10). So we’re not running or compiling the code—we’re only analyzing its *structure* (how big, how many methods, how many parameters) to estimate complexity.

---

## What is CORS?

**CORS (Cross-Origin Resource Sharing)** is a browser security rule. By default, a script on `http://localhost:5173` (frontend) cannot call `http://localhost:8080` (backend) because they are different **origins** (different port = different origin). The browser would block the request unless the backend explicitly allows it.

**In this project:** The backend sends the header `Access-Control-Allow-Origin: http://localhost:5173` (via Spring’s `@CrossOrigin`). That tells the browser: “Requests from that origin are allowed.” So the React app can call our API during development. Without CORS, the browser would block the call and you’d see a CORS error in the console.

---

## What is .env and why use it?

A **.env** file is a plain text file that holds **environment variables** (key=value pairs), often used for configuration and secrets (e.g. API keys). Many frameworks support loading it so you don’t hardcode secrets in code.

**In this project:** We put `LLM_API_URL`, `LLM_API_KEY`, and `LLM_MODEL` in a `.env` file in the project root. Spring doesn’t load `.env` by default, so we use **dotenv-java** in `main()` *before* starting Spring: we read `.env` and set those keys as **system properties**. Then `application.properties` uses `${LLM_API_URL:}` etc., which read from system properties. So the key never appears in code or in version control (`.env` is gitignored). If someone asks “Where is the API key?”—it’s in `.env` on the machine running the app, not in the repo.

---

## What is Vite?

**Vite** is a build tool and dev server for front-end projects. It uses **native ES modules** in the browser during development, so it can start and hot-reload very quickly. For production, it bundles your code (e.g. React, TypeScript) into optimized files.

**In this project:** We use Vite to run the React app (`npm run dev` → dev server on port 5173), to compile TypeScript, and to inject `VITE_API_BASE_URL` via `import.meta.env` so the app knows which backend URL to call.

---

## What is TypeScript?

**TypeScript** is JavaScript with **static types**. You can declare types for variables, function parameters, and return values (e.g. `data: AnalysisResponse | null`). The TypeScript compiler checks that you use data consistently and gives better editor support and fewer runtime type errors.

**In this project:** The frontend is written in TypeScript. We define types like `FileRisk` and `AnalysisResponse` to match the backend JSON. That way, when we use `data.fileRisks` or `resp.data.steps`, the editor and compiler know the shape of the data.

---

## What is Maven?

**Maven** is a build and dependency-management tool for Java. You describe the project in `pom.xml` (project object model): dependencies (e.g. Spring Boot, JGit), plugins (compiler, Spring Boot repackager), and version settings. Running `mvn package` compiles the code, runs tests, and produces a JAR (or WAR). Maven also downloads dependencies from central repositories.

**In this project:** We use Maven to build the backend. `mvn -B -DskipTests package` produces `target/repo-risk-analyzer-backend.jar`, which we run with `java -jar ...`. All backend libraries (Spring, JGit, JDT, dotenv, etc.) are declared in `pom.xml`.

---

# PART B — PROJECT-SPECIFIC DETAIL (What We Built & How)

---

## 1. What is FaultLine?

FaultLine is a full-stack “seismic sensor” for Java codebases. It:

1. **Analyzes Git history** (via JGit) to find hot files: modification count, lines changed (churn), and number of unique contributors per file.
2. **Parses Java source** (via Eclipse JDT) to measure structural complexity: class size, method count, long methods, and parameter count, combined into an AST score (0–10).
3. **Combines** behavioral (churn) and structural (AST) data into a **single risk score (0–10)** per file using fixed weights (e.g. 35% modifications, 20% contributors, 25% churn, 20% AST).
4. **Optionally** calls a **generative AI** (LLM) API to produce natural-language insights and per-file “steps to fix.”

There is no database; each analysis is stateless.

---

## 2. Architecture

- **Backend:** Java 17, Spring Boot. REST API; uses JGit for Git, Eclipse JDT for Java AST, WebClient for LLM calls, dotenv-java for `.env`.
- **Frontend:** React 18, TypeScript, Vite, Axios, plain CSS. Single-page app: form → call `/api/analyze` → show dashboard (report cards with round bars, file table, insights). Click file → call `/api/insights/file` → show AI steps.
- **Data flow:** User input (URL or path) → backend clones or uses local path → walks commits, computes churn and contributors → parses `.java` files for AST → computes risk per file → optionally calls LLM for insights → returns JSON → frontend renders.

---

## 3. Backend — Technologies and Why

| Technology | What it is | Why we use it |
|------------|------------|----------------|
| Java 17 | LTS Java | Required by Eclipse JDT (class file 61). |
| Spring Boot 2.7 | Java web framework | Fast setup, embedded server, DI, REST, property binding. |
| JGit | Git in Java | Clone and walk history without `git` CLI; cross-platform. |
| Eclipse JDT | Java AST library | Parse Java source for class/method/parameter metrics. |
| WebClient (WebFlux) | Reactive HTTP client | Call LLM API with timeouts and error handling. |
| dotenv-java | .env loader | Load LLM URL/key/model from file into system properties. |
| Jackson | JSON | Serialize/deserialize requests and responses; parse LLM JSON. |
| Bean Validation | @Valid, @NotBlank, @Min | Validate `AnalysisRequest` before processing. |

---

## 4. Backend — Components

### RepoRiskAnalyzerApplication (entry point)

- Runs before Spring: loads `.env` from current directory and parent (so it works when run from `target/`), sets only non-empty values as system properties. Then starts Spring. So `llm.api.url` etc. are available when `LlmService` is created.

### application.properties

- `server.port=8080`, logging levels, `llm.api.url=${LLM_API_URL:}`, `llm.api.key`, `llm.model`. LLM defaults to empty so the app runs without an LLM until the user configures `.env`.

### RepoAnalysisController

- **POST /api/analyze:** Body `{ "repoPath", "maxCommits" }`. Validates via `@Valid`, calls `RepoAnalysisService.analyzeRepository`, returns `AnalysisResponse` or 400/500.
- **GET /api/health:** Returns plain text.
- **GET /api/health/llm:** If LLM not configured → 503. Else calls `LlmService.testConnection()` → 200 or 502.
- **POST /api/insights/file:** Body `{ "repositoryPath", "file": { path, riskScore, modifications, contributors, churnLines, astSizeScore } }`. If LLM not configured → 503. Else calls `LlmService.generateFixSteps` → 200 `{ "steps": [...] }` or 502 with error message.
- CORS allows `http://localhost:3000` and `http://localhost:5173`.

### RepoAnalysisService

- **Validate:** `repoPath` not blank, `maxCommits >= 1`.
- **Resolve repo:** If input looks like URL (e.g. contains `://` or `github.com` or ends with `.git`), clone with JGit into a temp directory; else use as local path. If local path doesn’t exist but looks like URL, try cloning.
- **Open repo:** `FileRepositoryBuilder` to get `Repository`.
- **Traverse commits:** `RevWalk` from HEAD, up to `maxCommits`. For each commit, diff against parent: update per-file modification count, add churn (sum of edit sizes from `FileHeader.toEditList()`), add author to per-file contributor set.
- **AST (Java only):** For each path in the modification set that ends with `.java`, read file, parse with JDT `ASTParser` (JLS8, compilation unit), run `AstMetricsVisitor` (class lines, method count, long methods >50 estimated lines, total parameters). Formula: `classScore = log1p(classLines)/5`, `methodScore = log1p(methodCount)/3`, `longMethodScore = log1p(longMethodCount)/2`, `paramScore = log1p(params)/4`; `raw = sum`; `astScore = min(10, raw*2)`.
- **Risk per file:** `normMods = log1p(mods)/log1p(50)`, `normContrib = log1p(contrib)/log1p(20)`, `normChurn = log1p(churn)/log1p(1000)`, `normAst = astScore/10`. Weights: 0.35, 0.20, 0.25, 0.20. `score = (weighted sum)*10`, clamp [0,10]. Build `FileRisk` for each file, sort by risk descending.
- **Insights:** If LLM enabled, `llmService.generateInsights(top 10)`; if empty, use heuristic (top 5, hotspots, high-AST, recommendations). If LLM disabled, heuristic only.
- **Cleanup:** If we created a temp clone, delete it recursively.

### LlmService

- **Config:** `llm.api.url`, `llm.api.key`, `llm.model` from properties. `isEnabled()` true only if URL and key are non-blank.
- **testConnection():** Minimal prompt (“Reply with exactly: OK”), POST to LLM, parse `choices[0].message.content` (or array of text parts), return 200 with reply or 502 with error.
- **generateInsights(topFiles):** Build prompt with file paths and metrics; system prompt asks for 2–4 bullets per file in simple language. POST, parse content, split by newlines → list of insight strings.
- **generateFixSteps(file, repoPath):** Prompt: give 4–6 numbered steps to fix this file given repo path, file path, and metrics. System: “Reply only with numbered steps. No markdown.” POST, parse content (string or array via `extractMessageContent`), split by newline; if no lines, split by pattern `\s*\d+\.\s*`. Return list of steps; on API error or empty, throw so controller returns 502.
- **WebClient:** Uses `onStatus` to map 4xx/5xx body to a clear error (e.g. `error.message`), then throw. Timeouts 15–30 s; we `.block()` so the controller stays synchronous.

### Models

- **AnalysisRequest:** `repoPath` (required), `maxCommits` (default 1000, min 1).
- **AnalysisResponse:** `repositoryPath`, `commitsAnalyzed`, `fileRisks`, `insights`.
- **FileRisk:** `path`, `modifications`, `contributors`, `churnLines`, `astSizeScore`, `riskScore`.

---

## 5. Frontend — Technologies and Why

| Technology | What it is | Why we use it |
|------------|------------|----------------|
| React 18 | UI library | Components, state, hooks. |
| TypeScript | Typed JS | Types for API and state. |
| Vite 5 | Build/dev server | Fast dev, env vars. |
| Axios | HTTP client | POST/GET, error handling. |
| Plain CSS | Custom styles | Theme, animations, round bars. |

---

## 6. Frontend — Behavior

- **State:** `repoPath`, `maxCommits`, `loading`, `error`, `data`, `selectedFile`, `fileSteps`, `loadingSteps`, `stepsError`.
- **Form submit:** `POST /api/analyze` with `repoPath`, `maxCommits`. On success set `data`; on error set `error` from response body or message.
- **Report dashboard (when `data` exists):** From `data.fileRisks` compute: low (risk &lt; 4), medium (4–7), high (≥7) counts; average risk; max risk; percentages. Five cards: each has a round bar (conic-gradient with `--p` for percentage, inner circle for ring) and center value (avg risk, counts, or max). Colours: accent, green, yellow, pink.
- **Table:** Rows = `data.fileRisks`, sorted by risk. Each row clickable; onClick calls `POST /api/insights/file` with `repositoryPath` and that file’s object. Set `selectedFile`, then `fileSteps` or `stepsError`. Selected row has `selected` class.
- **Steps list:** Strip leading “1.” “2.” from each step string so `<ol>` doesn’t double-number.
- **Errors:** Show backend message; if “not configured” show .env hint; if “429” or “quota” show billing link.

---

## 7. Configuration

| Where | What | Purpose |
|-------|------|--------|
| Backend `.env` (project root) | `LLM_API_URL`, `LLM_API_KEY`, `LLM_MODEL` | LLM config; loaded before Spring. |
| application.properties | `server.port`, `logging.*`, `llm.api.*` | Port, logs, LLM from env. |
| Frontend `.env` (optional) | `VITE_API_BASE_URL` | Backend API base (default localhost:8080/api). |
| Controller | `@CrossOrigin(origins = {...})` | Allow browser from dev server. |

---

## 8. API Summary

| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/analyze` | `{ "repoPath", "maxCommits" }` | 200: `{ repositoryPath, commitsAnalyzed, fileRisks[], insights[] }` |
| GET | `/api/health` | — | 200: text |
| GET | `/api/health/llm` | — | 200: `{ ok, message, reply? }` or 503/502 |
| POST | `/api/insights/file` | `{ "repositoryPath", "file" }` | 200: `{ "steps": string[] }` or 503/502 |

---

## 9. FAQ — Short Answers

- **Why Java 17?** JDT requires it (class file 61).
- **Why JGit?** No `git` binary; same on all OS; clone and history in process.
- **Why JDT?** Standard Java AST; we need structure without full compile.
- **Why 0–10 risk?** Single number; normalization and weights balance metrics.
- **Why URL and local path?** URL = no user clone; local = faster when repo exists.
- **Why delete temp clone?** Save disk; analysis is stateless.
- **Why WebClient for LLM?** Non-blocking, timeouts, good errors.
- **Why .env?** Secrets in file, not in code; gitignored.
- **Why React?** Simple SPA; README Angular badge is historical.
- **Why round bars?** Quick visual for avg, distribution, peak.
- **Why strip "1." from steps?** `<ol>` already numbers; avoid “1. 1. …”.

---

## 10. File Map

```
FaultLine/
├── pom.xml
├── .env.example / .env
├── FaultLine_Complete_Reference_Download.md  (this file)
├── src/main/java/.../reporisk/
│   ├── RepoRiskAnalyzerApplication.java
│   ├── controller/RepoAnalysisController.java
│   ├── model/ (AnalysisRequest, AnalysisResponse, FileRisk)
│   └── service/ (RepoAnalysisService, LlmService)
├── src/main/resources/application.properties
└── frontend/
    ├── package.json, index.html, vite.config.ts, tsconfig.json
    └── src/main.tsx, App.tsx, styles.css
```

---

**To use:** Download this file (e.g. right-click → Save As, or copy to a private repo). Keep it for your own reference and for answering questions about concepts and implementation.
