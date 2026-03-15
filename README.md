<p align="center">
<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00d4ff,100:2575fc&height=200&section=header&text=FaultLine&fontSize=45&fontColor=ffffff"/>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-00d4ff?style=for-the-badge&logo=openjdk&logoColor=black" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-000000?style=for-the-badge&logo=spring&logoColor=00d4ff" />
  <img src="https://img.shields.io/badge/Angular-15-00d4ff?style=for-the-badge&logo=angular&logoColor=black" />
  <img src="https://img.shields.io/badge/LLM-Ready-000000?style=for-the-badge&logo=openai&logoColor=00d4ff" />
</p>

## 🔎 What is FaultLine?
**FaultLine** is a "Seismic Sensor" for codebases. It is a full-stack engineering tool that identifies hidden technical debt by merging a repository's **behavioral history** with its **structural complexity.** While most tools just look at the code, FaultLine looks at the *story* of the code to find the "Faults" before they lead to production outages.

---

## 🛠️ How it Works (The Core Logic)
FaultLine calculates a **Risk Score (0-100)** by analyzing two distinct data dimensions:

1.  **Commit Churn (Behavioral Analysis):** Using **JGit**, it traverses the repository's history to find "Hotspots"—files that change constantly or are touched by too many developers.
2.  **AST Metrics (Structural Analysis):** Using **Eclipse JDT**, it parses Java source code into an *Abstract Syntax Tree* to measure deep metrics like method length, class nesting, and parameter bloat.
3.  **AI Interpretation:** Once the data is gathered, an **LLM** generates natural language insights, telling you *exactly* why a file is risky and how to refactor it.

---

## 🌟 Why is this helpful?
* **For Team Leads:** Instantly identify which modules are becoming "God Objects" and slowing down the team.
* **For Developers:** Get AI-assisted refactoring suggestions that go beyond simple "linting" rules.
* **For Onboarding:** Help new engineers visualize the most complex parts of a legacy codebase through the **Seismic Risk Dashboard.**

---

## 🎯 Key Technical Highlights
- 🔄 **Real-time Sync**: Async processing with **SSE (Server-Sent Events)** updates so you can watch the analysis happen live.
- 🧠 **Context-Aware AI**: It doesn't just flag long methods; it uses repo-wide metrics to give actionable recommendations.
- 🐳 **Portable Architecture**: Fully Dockerized and demo-ready with an internal H2 database.

---

## 🎨 Visuals — Risk Legend
| Level | Description | Status |
| :--- | :--- | :--- |
| 🟢 **LOW** | Stable code with low complexity. | Safe to scale. |
| 🟡 **MEDIUM** | Moderate churn or rising complexity. | Monitor for debt. |
| 🔴 **HIGH** | "The FaultLine"—Frequent changes + Messy logic. | **Refactor ASAP.** |

---

## 🚀 How to run the project

Follow these steps to run FaultLine locally (backend + frontend).

### Prerequisites

- **Java 17** (required for the backend). Check with `java -version`.
- **Maven 3.6+** (to build the backend). Check with `mvn -v`.
- **Node.js 18+** and **npm** (for the frontend). Check with `node -v` and `npm -v`.

### Step 1: Clone and open the project

```bash
git clone <your-repo-url>
cd FaultLine
```

### Step 2: Configure the backend (optional but recommended)

To enable **AI “Steps to fix”** when you click a file, set your LLM API key in a `.env` file in the **project root** (same folder as `pom.xml`):

1. Copy the example: `cp .env.example .env`
2. Edit `.env` and set at least:
   - `LLM_API_URL` – e.g. `https://api.groq.com/openai/v1/chat/completions` (free) or `https://api.openai.com/v1/chat/completions`
   - `LLM_API_KEY` – your API key from [Groq](https://console.groq.com), [OpenRouter](https://openrouter.ai/keys), or [OpenAI](https://platform.openai.com/api-keys)
   - `LLM_MODEL` – e.g. `llama-3.3-70b-versatile` (Groq) or `gpt-4o` (OpenAI)

If you skip this, analysis and the risk dashboard still work; only the per-file AI fix steps will be disabled.

### Step 3: Build and run the backend

From the **project root** (the `FaultLine` folder):

```bash
mvn -B -DskipTests package
java -jar target/repo-risk-analyzer-backend.jar
```

- Wait until you see the app start (e.g. “Started RepoRiskAnalyzerApplication”).
- Backend runs at **http://localhost:8080**.
- Optional: open **http://localhost:8080/api/health** and **http://localhost:8080/api/health/llm** in a browser to confirm the server and (if configured) LLM are OK.

### Step 4: Run the frontend (Angular)

Open a **new terminal**, go to the frontend folder, install dependencies, and start the Angular dev server:

```bash
cd frontend
npm install
npm start
```

- When the Angular CLI is ready, open **http://localhost:4200** in your browser.

### Step 5: Use the app

1. In the UI, paste a **Git repo URL** (e.g. `https://github.com/ataraxie/codeshovel.git`) or a **local path** to a Git repo.
2. Optionally change **Max commits to analyze**.
3. Click **Run analysis**.
4. When the run finishes, you’ll see the **Seismic Risk Dashboard**: report cards with round bars, a file risk table, and insights.
5. **Click a file** in the table to get AI-generated “Steps to fix” (if you configured an LLM in Step 2).

---

### Backend API (reference)

- `GET /api/health` – health check.
- `GET /api/health/llm` – test LLM configuration.
- `POST /api/analyze` – run analysis. Body: `{ "repoPath": "https://github.com/.../repo.git" or "/path/to/repo", "maxCommits": 1000 }`.
- `POST /api/insights/file` – get AI fix steps for a file (body includes `repositoryPath` and `file` with risk metrics).

---

## 🐳 Docker (Backend only)

To build and run the backend in Docker:

```bash
docker-compose build
docker-compose up
```

This exposes the backend on `http://localhost:8080`. You still need to run the Angular frontend separately (Step 4 above); it runs on **http://localhost:4200** and talks to the backend.

---

## 🤖 Free LLM options (for “Steps to fix”)

The app uses any **OpenAI-compatible** chat API. You can switch to a free provider by changing `.env`:

| Provider | Free tier | Get key | Example `.env` |
|----------|-----------|---------|----------------|
| **Groq** | Generous free rate limits | [console.groq.com](https://console.groq.com) | `LLM_API_URL=https://api.groq.com/openai/v1/chat/completions`, `LLM_MODEL=llama-3.3-70b-versatile` |
| **OpenRouter** | Many free models | [openrouter.ai/keys](https://openrouter.ai/keys) | `LLM_API_URL=https://openrouter.ai/api/v1/chat/completions`, `LLM_MODEL=google/gemma-2-9b-it:free` |
| **OpenAI** | Limited free credits | [platform.openai.com](https://platform.openai.com/api-keys) | `LLM_API_URL=https://api.openai.com/v1/chat/completions`, `LLM_MODEL=gpt-4o` |

Copy the URL and model into your `.env`, set `LLM_API_KEY` to the key from that provider, then restart the backend. No code changes needed.


