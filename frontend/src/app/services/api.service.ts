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

export interface CodeSnippet {
  description: string;
  current: string;
  suggested: string;
}

export interface FixStepsResponse {
  steps: string[];
  snippets?: CodeSnippet[];
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

  getFileContent(repositoryPath: string, filePath: string): Observable<{ content: string }> {
    const params = { repositoryPath, filePath };
    return this.http.get<{ content: string }>(`${this.apiBase}/file-content`, { params });
  }

  getFixSteps(
    repositoryPath: string,
    file: FileRisk,
    fileContent?: string | null
  ): Observable<FixStepsResponse> {
    const body: Record<string, unknown> = {
      repositoryPath,
      file: {
        path: file.path,
        riskScore: file.riskScore,
        modifications: file.modifications,
        contributors: file.contributors,
        churnLines: file.churnLines,
        astSizeScore: file.astSizeScore,
      },
    };
    if (fileContent != null && fileContent.trim() !== '') {
      body['fileContent'] = fileContent;
    }
    return this.http.post<FixStepsResponse>(`${this.apiBase}/insights/file`, body);
  }
}
