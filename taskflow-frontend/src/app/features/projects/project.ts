import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { ProjectRequest, ProjectResponse } from '../../core/models/project.model';
import { Observable } from 'rxjs';

@Service()
export class Project {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/projects';

  readonly #projects = signal<ProjectResponse[]>([]);
  readonly #loading = signal(false);
  readonly #error = signal<string | null>(null);

  readonly projects = this.#projects.asReadonly();
  readonly loading = this.#loading.asReadonly();
  readonly error = this.#error.asReadonly();

  loadProjects(): void {
    this.#loading.set(true);
    this.#error.set(null);

    this.http.get<ProjectResponse[]>(this.baseUrl).subscribe({
      next: (projects) => {
        this.#projects.set(projects);
        this.#loading.set(false);
      },
      error: () => {
        this.#error.set('Failed to load projects.');
        this.#loading.set(false);
      },
    });
  }

  createProject(request: ProjectRequest): Observable<ProjectResponse> {
    return this.http.post<ProjectResponse>(this.baseUrl, request);
  }

  updateProject(projectId: number, request: ProjectRequest): Observable<ProjectResponse> {
    return this.http.put<ProjectResponse>(`${this.baseUrl}/${projectId}`, request);
  }

  deleteProject(projectId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${projectId}`);
  }
}
