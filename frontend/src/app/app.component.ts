import { Component } from '@angular/core';
import { ApiService, AnalysisResponse, FileRisk } from './services/api.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  repoPath = '';
  maxCommits = 500;
  loading = false;
  error: string | null = null;
  data: AnalysisResponse | null = null;
  selectedFile: FileRisk | null = null;
  fileSteps: string[] = [];
  loadingSteps = false;
  stepsError: string | null = null;

  constructor(private api: ApiService) {}

  riskLevel(score: number): string {
    if (score >= 7) return 'high';
    if (score >= 4) return 'medium';
    return 'low';
  }

  onSubmit(): void {
    this.error = null;
    this.loading = true;
    this.data = null;
    this.selectedFile = null;
    this.fileSteps = [];
    this.stepsError = null;

    this.api.analyze(this.repoPath, this.maxCommits).subscribe({
      next: (res) => {
        this.data = res;
        this.loading = false;
      },
      error: (err) => {
        const d = err?.error;
        if (typeof d === 'string') this.error = d;
        else if (d?.message) this.error = d.message;
        else this.error = err?.message || 'Failed to run analysis. Is the backend running on :8080?';
        this.loading = false;
      },
    });
  }

  onFileClick(file: FileRisk): void {
    this.selectedFile = file;
    this.fileSteps = [];
    this.stepsError = null;
    this.loadingSteps = true;

    const repoPath = this.data?.repositoryPath ?? '';
    this.api.getFixSteps(repoPath, file).subscribe({
      next: (res) => {
        this.fileSteps = res?.steps ?? [];
        this.loadingSteps = false;
      },
      error: (err) => {
        const d = err?.error;
        this.stepsError = typeof d === 'string' ? d : d?.message ?? err?.message ?? 'Could not load fix steps.';
        this.fileSteps = [];
        this.loadingSteps = false;
      },
    });
  }

  backToInsights(): void {
    this.selectedFile = null;
    this.fileSteps = [];
    this.stepsError = null;
  }

  stripStepNumber(step: string): string {
    const stripped = step.replace(/^\s*\d+[.)]\s*/, '').trim();
    return stripped || step;
  }

  trackByPath(_index: number, item: FileRisk): string {
    return item.path;
  }

  get reportStats(): {
    lowCount: number;
    medCount: number;
    highCount: number;
    avgRisk: number;
    maxRisk: number;
    overallPct: number;
    lowPct: number;
    medPct: number;
    highPct: number;
    fileCount: number;
  } | null {
    if (!this.data?.fileRisks?.length) return null;
    const files = this.data.fileRisks;
    const lowCount = files.filter((f) => f.riskScore < 4).length;
    const medCount = files.filter((f) => f.riskScore >= 4 && f.riskScore < 7).length;
    const highCount = files.filter((f) => f.riskScore >= 7).length;
    const avgRisk = files.reduce((s, f) => s + f.riskScore, 0) / files.length;
    const maxRisk = Math.max(...files.map((f) => f.riskScore));
    const n = files.length;
    return {
      lowCount,
      medCount,
      highCount,
      avgRisk,
      maxRisk,
      overallPct: Math.min(100, (avgRisk / 10) * 100),
      lowPct: (lowCount / n) * 100,
      medPct: (medCount / n) * 100,
      highPct: (highCount / n) * 100,
      fileCount: n,
    };
  }
}
