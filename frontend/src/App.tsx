import { useState } from "react";
import axios from "axios";

type FileRisk = {
  path: string;
  modifications: number;
  contributors: number;
  churnLines: number;
  astSizeScore: number;
  riskScore: number;
};

type AnalysisResponse = {
  repositoryPath: string;
  commitsAnalyzed: number;
  fileRisks: FileRisk[];
  insights: string[];
};

const API_BASE =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

export function App() {
  const [repoPath, setRepoPath] = useState("");
  const [maxCommits, setMaxCommits] = useState(500);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<AnalysisResponse | null>(null);
  const [selectedFile, setSelectedFile] = useState<FileRisk | null>(null);
  const [fileSteps, setFileSteps] = useState<string[]>([]);
  const [loadingSteps, setLoadingSteps] = useState(false);
  const [stepsError, setStepsError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    setData(null);
    setSelectedFile(null);
    setFileSteps([]);
    setStepsError(null);
    try {
      const resp = await axios.post<AnalysisResponse>(`${API_BASE}/analyze`, {
        repoPath,
        maxCommits,
      });
      setData(resp.data);
    } catch (err: any) {
      let msg: string;
      if (err?.response) {
        const data = err.response.data;
        if (typeof data === "string") {
          msg = data;
        } else if (data && typeof data.message === "string") {
          msg = data.message;
        } else {
          msg = "Backend error: " + JSON.stringify(data);
        }
      } else if (err?.message) {
        msg = err.message;
      } else {
        msg = "Failed to run analysis. Is the backend running on :8080?";
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const riskLevel = (score: number) => {
    if (score >= 7) return "high";
    if (score >= 4) return "medium";
    return "low";
  };

  const handleFileClick = async (file: FileRisk) => {
    setSelectedFile(file);
    setFileSteps([]);
    setStepsError(null);
    setLoadingSteps(true);
    try {
      const resp = await axios.post<{ steps: string[] }>(`${API_BASE}/insights/file`, {
        repositoryPath: data?.repositoryPath ?? "",
        file: {
          path: file.path,
          riskScore: file.riskScore,
          modifications: file.modifications,
          contributors: file.contributors,
          churnLines: file.churnLines,
          astSizeScore: file.astSizeScore,
        },
      });
      setFileSteps(resp.data?.steps ?? []);
    } catch (err: any) {
      const data = err?.response?.data;
      setStepsError(
        typeof data === "string"
          ? data
          : data?.message ?? err?.message ?? "Could not load fix steps."
      );
      setFileSteps([]);
    } finally {
      setLoadingSteps(false);
    }
  };

  return (
    <div className="app-root">
      <header className="app-header">
        <div>
          <h1>FaultLine</h1>
          <p className="subtitle">
            Seismic Sensor for your Java repositories – detect hidden technical
            debt before it erupts.
          </p>
        </div>
      </header>

      <main className="app-main">
        <section className="panel">
          <h2>Analyze a repository</h2>
          <p className="panel-helper">
            Paste a public Git URL (for example{" "}
            <span className="mono">
              https://github.com/ataraxie/codeshovel.git
            </span>{" "}
            ) or point to a local Git repository path. FaultLine will clone or
            read it, then compute a 0–10 risk score that merges commit churn
            with structural complexity.
          </p>
          <form className="form" onSubmit={handleSubmit}>
            <label className="field">
              <span>Repository URL or path</span>
              <input
                type="text"
                placeholder="https://github.com/ataraxie/codeshovel.git"
                value={repoPath}
                onChange={(e) => setRepoPath(e.target.value)}
                required
              />
            </label>
            <label className="field inline">
              <span>Max commits to analyze</span>
              <input
                type="number"
                min={1}
                max={5000}
                value={maxCommits}
                onChange={(e) => setMaxCommits(Number(e.target.value || 1))}
              />
            </label>
            <button type="submit" disabled={loading || !repoPath}>
              {loading ? "Analyzing…" : "Run analysis"}
            </button>
          </form>
          {error && <div className="error-banner">{error}</div>}
        </section>

        {data && (() => {
          const files = data.fileRisks;
          const lowCount = files.filter((f) => f.riskScore < 4).length;
          const medCount = files.filter((f) => f.riskScore >= 4 && f.riskScore < 7).length;
          const highCount = files.filter((f) => f.riskScore >= 7).length;
          const avgRisk = files.length
            ? files.reduce((s, f) => s + f.riskScore, 0) / files.length
            : 0;
          const maxRisk = files.length ? Math.max(...files.map((f) => f.riskScore)) : 0;
          const overallPct = Math.min(100, (avgRisk / 10) * 100);
          const lowPct = files.length ? (lowCount / files.length) * 100 : 0;
          const medPct = files.length ? (medCount / files.length) * 100 : 0;
          const highPct = files.length ? (highCount / files.length) * 100 : 0;
          return (
          <section className="results">
            <div className="results-header">
              <div>
                <h2>Seismic Risk Dashboard</h2>
                <p className="muted">
                  {data.repositoryPath} • {data.commitsAnalyzed} commits
                  analyzed
                </p>
              </div>
              <div className="legend">
                <span className="pill pill-low">🟢 Low</span>
                <span className="pill pill-medium">🟡 Medium</span>
                <span className="pill pill-high">🔴 High</span>
              </div>
            </div>

            <div className="report-dashboard">
              <div className="report-card overall">
                <div className="report-card-title">Avg risk (0–10)</div>
                <div className="round-bar">
                  <div className="round-bar-fill" style={{ ["--p" as string]: overallPct }} />
                  <div className="round-bar-inner" />
                  <span className="round-bar-value">{avgRisk.toFixed(1)}</span>
                </div>
                <div className="report-card-label">across {files.length} files</div>
              </div>
              <div className="report-card low">
                <div className="report-card-title">Low risk</div>
                <div className="round-bar">
                  <div className="round-bar-fill" style={{ ["--p" as string]: lowPct }} />
                  <div className="round-bar-inner" />
                  <span className="round-bar-value">{lowCount}</span>
                </div>
                <div className="report-card-label">files</div>
              </div>
              <div className="report-card medium">
                <div className="report-card-title">Medium risk</div>
                <div className="round-bar">
                  <div className="round-bar-fill" style={{ ["--p" as string]: medPct }} />
                  <div className="round-bar-inner" />
                  <span className="round-bar-value">{medCount}</span>
                </div>
                <div className="report-card-label">files</div>
              </div>
              <div className="report-card high">
                <div className="report-card-title">High risk</div>
                <div className="round-bar">
                  <div className="round-bar-fill" style={{ ["--p" as string]: highPct }} />
                  <div className="round-bar-inner" />
                  <span className="round-bar-value">{highCount}</span>
                </div>
                <div className="report-card-label">refactor soon</div>
              </div>
              <div className="report-card">
                <div className="report-card-title">Peak risk</div>
                <div className="round-bar">
                  <div
                    className="round-bar-fill"
                    style={{ ["--p" as string]: (maxRisk / 10) * 100, ["--bar-color" as string]: "var(--danger)" }}
                  />
                  <div className="round-bar-inner" />
                  <span className="round-bar-value">{maxRisk.toFixed(1)}</span>
                </div>
                <div className="report-card-label">max score</div>
              </div>
            </div>

            <div className="layout-two-col">
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>File</th>
                      <th>Risk</th>
                      <th>Mods</th>
                      <th>Contributors</th>
                      <th>Churn</th>
                      <th>AST score</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.fileRisks.map((f) => (
                      <tr
                        key={f.path}
                        className={`${riskLevel(f.riskScore)} ${selectedFile?.path === f.path ? "selected" : ""} clickable`}
                        onClick={() => handleFileClick(f)}
                        role="button"
                        tabIndex={0}
                        onKeyDown={(e) => e.key === "Enter" && handleFileClick(f)}
                      >
                        <td className="mono">{f.path}</td>
                        <td>{f.riskScore.toFixed(2)}</td>
                        <td>{f.modifications}</td>
                        <td>{f.contributors}</td>
                        <td>{f.churnLines}</td>
                        <td>{f.astSizeScore.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="insights">
                <h3>Insights</h3>
                {data.insights.length === 0 && !selectedFile && (
                  <p className="muted">
                    No insights generated. Try increasing the commit window.
                  </p>
                )}
                {!selectedFile && (
                  <ul>
                    {data.insights.map((line, idx) => (
                      <li key={idx}>{line}</li>
                    ))}
                  </ul>
                )}
                {selectedFile && (
                  <div className="file-fix-panel">
                    <h4 className="file-fix-title">
                      Steps to fix: <span className="mono">{selectedFile.path}</span>
                    </h4>
                    <button
                      type="button"
                      className="back-link"
                      onClick={() => { setSelectedFile(null); setFileSteps([]); setStepsError(null); }}
                    >
                      ← Back to insights
                    </button>
                    {loadingSteps && (
                      <p className="muted">Asking AI for steps…</p>
                    )}
                    {stepsError && (
                      <div className="steps-error-box">
                        <p className="steps-error">{stepsError}</p>
                        {stepsError.toLowerCase().includes("not configured") && (
                          <p className="steps-error-hint">
                            In the project root (folder with <code>pom.xml</code>), edit{" "}
                            <code>.env</code> and set <code>LLM_API_KEY</code> to your real API key
                            (get one at platform.openai.com/api-keys). Keep{" "}
                            <code>LLM_API_URL=https://api.openai.com/v1/chat/completions</code>.
                            Restart the backend after saving.
                          </p>
                        )}
                        {(stepsError.includes("429") || stepsError.toLowerCase().includes("quota")) && (
                          <p className="steps-error-hint">
                            <strong>Quota / billing:</strong> Add a payment method or upgrade your plan at{" "}
                            <a href="https://platform.openai.com/account/billing" target="_blank" rel="noopener noreferrer">platform.openai.com/account/billing</a>.
                            Free-tier limits reset over time; see{" "}
                            <a href="https://platform.openai.com/docs/guides/error-codes/api-errors" target="_blank" rel="noopener noreferrer">API errors</a>.
                          </p>
                        )}
                      </div>
                    )}
                    {!loadingSteps && !stepsError && fileSteps.length > 0 && (
                      <ol className="fix-steps">
                        {fileSteps.map((step, idx) => (
                          <li key={idx}>
                            {step.replace(/^\s*\d+[.)]\s*/, "").trim() || step}
                          </li>
                        ))}
                      </ol>
                    )}
                    {!loadingSteps && !stepsError && fileSteps.length === 0 && (
                      <p className="muted">No steps returned. Check that LLM is configured.</p>
                    )}
                  </div>
                )}
              </div>
            </div>
          </section>
          );
        })()}
      </main>

      <footer className="app-footer">
        <span>Backend: Spring Boot on :8080 • Frontend: Vite + React on :5173</span>
      </footer>
    </div>
  );
}

