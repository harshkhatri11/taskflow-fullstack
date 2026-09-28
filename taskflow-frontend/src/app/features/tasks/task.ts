import { inject, Service, signal } from '@angular/core';
import {
  PageInfo,
  SpringPage,
  TaskRequest,
  TaskResponse,
  TaskStatusUpdateRequest,
} from '../../core/models/task.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { catchError, map, Observable, tap, throwError } from 'rxjs';
import { Notification } from '../../shared/notification';
import { TaskStatus } from '../../core/models/enums';

@Service()
export class Task {
  private readonly http = inject(HttpClient);
  private readonly notification = inject(Notification);

  readonly #tasks = signal<TaskResponse[]>([]);
  readonly #pageInfo = signal<PageInfo>({
    totalElements: 0,
    totalPages: 0,
    pageNumber: 0,
    pageSize: 0,
    first: true,
    last: true,
  });
  readonly #loading = signal(false);
  readonly #error = signal<string | null>(null);

  readonly tasks = this.#tasks.asReadonly();
  readonly pageInfo = this.#pageInfo.asReadonly();
  readonly loading = this.#loading.asReadonly();
  readonly error = this.#error.asReadonly();

  /**
   * Loads one page of tasks for a project. Void — mutates internal state.
   * The component owns the requested  page/size/sort as local
   * signals (paginator/sort UI) and calls this
   * whenever they change; this only ever reflects what the backend
   * actually returned.
   */
  loadTasks(projectId: number, page: number, size: number, sort?: string): void {
    this.#loading.set(true);
    this.#error.set(null);

    let params = new HttpParams().set('page', page).set('size', size);
    if (sort) {
      params = params.set('sort', sort);
    }

    this.http.get<SpringPage<TaskResponse>>(`/projects/${projectId}/tasks`, { params }).subscribe({
      next: (result) => {
        this.#tasks.set(result.content);
        this.#pageInfo.set({
          totalElements: result.totalElements,
          totalPages: result.totalPages,
          pageNumber: result.number,
          pageSize: result.size,
          first: result.first,
          last: result.last,
        });
        this.#loading.set(false);
      },
      error: () => {
        this.#error.set('Failed to load tasks.');
        this.#loading.set(false);
      },
    });
  }

  /**
   * Full create/assign — ADMIN/MANAGER only (backend-enforced; the
   * assignedTo dropdown is also re-validated server-side). Raw Observable:
   * caller owns follow-up.
   */
  createTask(projectId: number, request: TaskRequest): Observable<TaskResponse> {
    return this.http.post<TaskResponse>(`/projects/${projectId}/tasks`, request);
  }

  /**
   * Full edit (PUT) — ADMIN/MANAGER only, never reachable by EMPLOYEE.
   * Raw Observable.
   */
  updateTask(taskId: number, request: TaskRequest): Observable<TaskResponse> {
    return this.http.put<TaskResponse>(`/tasks/${taskId}`, request);
  }

  /**
   * Narrow status-only PATCH, driven from an inline <mat-select> in a
   * TaskList row (EMPLOYEE-facing). Deliberately different error-handling
   * philosophy from create/updateTask above: no form-level error UI exists
   * for a table-row control, so failures are surfaced directly via
   * Notification here rather than left for the caller. Still returns the
   * Observable so the caller can clear its own local "saving" state
   * regardless of outcome.
   *
   * Rollback is a whole-array snapshot, not a per-task patch-back — safer
   * against the list having changed shape while the request was in flight.
   */
  updateTaskStatus(taskId: number, status: TaskStatus): Observable<TaskResponse> {
    const snapshot = this.#tasks();

    this.#tasks.set(snapshot.map((task) => (task.id === taskId ? { ...task, status } : task)));

    const request: TaskStatusUpdateRequest = { status };

    return this.http.patch<TaskResponse>(`/tasks/${taskId}/status`, request).pipe(
      tap((updated) => {
        this.#tasks.set(this.#tasks().map((task) => (task.id === taskId ? updated : task)));
      }),
      catchError((err) => {
        this.#tasks.set(snapshot);
        this.notification.showError('Could not update task status. Please try again.');
        return throwError(() => err);
      }),
    );
  }

  /**
   * ADMIN/MANAGER only. Raw Observable — caller re-fetches the current
   * page on success.
   */
  deleteTask(taskId: number): Observable<void> {
    return this.http.delete<void>(`/tasks/${taskId}`);
  }

  checkProjectAccess(projectId: number): Observable<boolean> {
    const params = new HttpParams().set('page', 0).set('size', 1);
    return this.http.get<SpringPage<TaskResponse>>(`/projects/${projectId}/tasks`, { params }).pipe(
      map(() => true), // Map successful 200 OK responses to true
    );
  }
}
