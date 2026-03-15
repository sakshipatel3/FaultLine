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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    setData(null);
    try {
      const resp = await axios.post<AnalysisResponse>(`${API_BASE}/analyze`, {
        repoPath,
        maxCommits,
      });
      setData(resp.data);
    } catch (err: any) {
      const msg =
        err?.response?.data ??
        err?.message ??
        "Failed to run analysis. Is the backend running on :8080?";
      setError(String(msg));
    } finally {
      setLoading(false);
    }
  };

  const riskLevel = (score: number) => {
    if (score >= 7) return "high";
    if (score >= 4) return "medium";
    return "low";
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
            Point FaultLine at a local Git repository and compute a 0–10 risk
            score that merges commit churn with structural complexity.
          </p>
          <form className="form" onSubmit={handleSubmit}>
            <label className="field">
              <span>Repository path</span>
              <input
                type="text"
                placeholder="/absolute/path/to/your/repo"
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

        {data && (
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
                      <tr key={f.path} className={riskLevel(f.riskScore)}>
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
                {data.insights.length === 0 && (
                  <p className="muted">
                    No insights generated. Try increasing the commit window.
                  </p>
                )}
                <ul>
                  {data.insights.map((line, idx) => (
                    <li key={idx}>{line}</li>
                  ))}
                </ul>
              </div>
            </div>
          </section>
        )}
      </main>

      <footer className="app-footer">
        <span>Backend: Spring Boot on :8080 • Frontend: Vite + React on :5173</span>
      </footer>
    </div>
  );
}

