import { inject, Service, signal } from '@angular/core';
import { CommentRequest, CommentResponse } from '../../core/models/comment.model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Service()
export class TaskComment {
  private readonly http = inject(HttpClient);

  readonly #comments = signal<CommentResponse[]>([]);
  readonly #loading = signal(false);
  readonly #error = signal<string | null>(null);

  readonly comments = this.#comments.asReadonly();
  readonly loading = this.#loading.asReadonly();
  readonly error = this.#error.asReadonly();

  /**
   * Loads the full comment thread for one task. Void — mutates internal
   * state. Not paginated — comment threads are bounded, unlike the task list.
   */
  loadComments(taskId: number): void {
    this.#loading.set(true);
    this.#error.set(null);

    this.http.get<CommentResponse[]>(`/tasks/${taskId}/comments`).subscribe({
      next: (comments) => {
        this.#comments.set(comments);
        this.#loading.set(false);
      },
      error: () => {
        this.#error.set('Failed to load comments.');
        this.#loading.set(false);
      },
    });
  }

  createComment(taskId: number, request: CommentRequest): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`/tasks/${taskId}/comments`, request);
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`/comments/${commentId}`);
  }
}
