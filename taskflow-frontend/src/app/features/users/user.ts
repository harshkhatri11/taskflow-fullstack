import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { Observable } from 'rxjs';
import {
  UserRequest,
  UserResponse,
  UserSummaryResponse,
  UserUpdateRequest,
} from '../../core/models/user.model';

@Service()
export class User {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/users';

  #users = signal<UserResponse[]>([]);
  readonly users = this.#users.asReadonly();

  #loading = signal(false);
  readonly loading = this.#loading.asReadonly();

  #error = signal<string | null>(null);
  readonly error = this.#error.asReadonly();

  getUsers(): Observable<UserSummaryResponse[]> {
    return this.http.get<UserSummaryResponse[]>(this.baseUrl);
  }

  loadUsers(): void {
    this.#loading.set(true);
    this.#error.set(null);

    this.http.get<UserResponse[]>(this.baseUrl).subscribe({
      next: (users) => {
        this.#users.set(users);
        this.#loading.set(false);
      },
      error: () => {
        this.#error.set('Failed to load users.');
        this.#loading.set(false);
      },
    });
  }

  createUser(request: UserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.baseUrl, request);
  }

  updateUser(userId: number, request: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/${userId}`, request);
  }

  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}`);
  }
}
