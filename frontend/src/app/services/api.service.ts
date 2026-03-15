import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';

export interface FileRisk {
  path: string;
  modifications: number;
  contributors: number;
  churnLines: number;
  astSizeScore: number;
  riskScore: number;
}

export interface AnalysisResponse {
  repositoryPath: string;
  commitsAnalyzed: number;
  fileRisks: FileRisk[];
  insights: string[];
}

export interface FixStepsResponse {
  steps: string[];
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly apiBase = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  analyze(repoPath: string, maxCommits: number): Observable<AnalysisResponse> {
    return this.http.post<AnalysisResponse>(`${this.apiBase}/analyze`, {
      repoPath,
      maxCommits,
    });
  }

  getFixSteps(repositoryPath: string, file: FileRisk): Observable<FixStepsResponse> {
    return this.http.post<FixStepsResponse>(`${this.apiBase}/insights/file`, {
      repositoryPath,
      file: {
        path: file.path,
        riskScore: file.riskScore,
        modifications: file.modifications,
        contributors: file.contributors,
        churnLines: file.churnLines,
        astSizeScore: file.astSizeScore,
      },
    });
  }
}
