import { inject, signal, computed, Service } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of, tap, throwError, timeout, TimeoutError } from 'rxjs';
import { Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';

import {
  AuthUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../models/auth.model';
import { Role } from '../models/enums';

// Raw JWT claim shape — exactly what jwt-decode hands back, matches
// JwtUtil's claims (sub/userId/role/iat/exp). Deliberately NOT exported
// and NOT in models/: nothing outside AuthService should ever read a
// raw claim. Everything else gets the mapped AuthUser instead — see
// toAuthUser() below. Keeping this un-exported is what enforces that.
interface JwtPayload {
  sub: string; // email
  userId: number;
  role: Role;
  iat: number;
  exp: number;
}

interface EmailCheckResponse {
  exists: boolean;
}

interface RefreshRequest {
  userId: number;
  refreshToken: string;
}

const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';

// Bound on how long app boot will wait for a silent session restore
// (restoreSession(), below) before giving up and letting the app render
// as logged-out. Only matters on a cold reload with an expired access
// token — every other boot path resolves synchronously, no network call.
const RESTORE_SESSION_TIMEOUT_MS = 5000;

@Service()
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // NOTE — storage decision, made deliberately, not by default:
  // Both tokens currently live in localStorage. Known trade-off: an XSS
  // payload can read them. Mitigated by short access-token TTL (15 min)
  // + refresh-token rotation on the backend (already implemented).
  // Stronger option not yet built: move the refresh token to an httpOnly
  // + Secure + SameSite=Strict cookie set by the backend, so JS can never
  // read it at all. That requires AuthController to set the cookie on
  // login/refresh instead of returning refreshToken in the JSON body,
  // plus CORS withCredentials on both sides. Revisit before shipping
  // anything beyond this practice build.

  #currentUser = signal<AuthUser | null>(this.getUserFromToken());

  readonly currentUser = this.#currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.#currentUser() !== null);

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/auth/login', credentials).pipe(
      tap((response) => {
        this.saveTokens(response.accessToken, response.refreshToken);
        this.#currentUser.set(this.toAuthUser(this.decodeToken(response.accessToken)));
      }),
    );
  }

  /**
   * Register a new user account. Role is server-assigned (EMPLOYEE by
   * default) regardless of what's sent
   */
  register(userData: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/auth/register', userData);
  }

  /**
   * Called only from authInterceptor's 401-handling branch, and from
   * restoreSession() below — never directly from components. Exchanges
   * the stored refresh token for a new access+refresh pair (backend
   * rotates both), persists them the same way login() does, and resolves
   * with just the new access token since that's all the interceptor
   * needs to retry the original request.
   *
   * userId is optional because the two callers are in different states:
   * the interceptor calls this mid-session, when #currentUser is still
   * populated from the (now-expired) token, so it can omit userId and
   * let it fall back to the signal. restoreSession() calls this on cold
   * boot, when #currentUser is null by definition — it decodes userId
   * out of the expired access token itself and passes it explicitly.
   */
  refreshAccessToken(userId?: number): Observable<string> {
    const refreshToken = this.getRefreshToken();
    const resolvedUserId = userId ?? this.#currentUser()?.userId;

    if (!refreshToken || resolvedUserId === undefined) {
      return throwError(() => new Error('No refresh token or userId available'));
    }

    const body: RefreshRequest = { userId: resolvedUserId, refreshToken };

    return this.http.post<LoginResponse>('/auth/refresh', body).pipe(
      tap((response) => {
        this.saveTokens(response.accessToken, response.refreshToken);
        this.#currentUser.set(this.toAuthUser(this.decodeToken(response.accessToken)));
      }),
      map((response) => response.accessToken),
    );
  }

  /**
   * Called once from provideAppInitializer in app.config.ts — never from
   * guards or components. Handles the one case the constructor's
   * getUserFromToken() deliberately leaves unresolved: an access token
   * that's expired, but a refresh token that's still good, sitting in
   * storage from a previous session (e.g. after a hard page reload more
   * than 15 minutes into a session).
   *
   * Always resolves, never errors — a failed or impossible restore just
   * means #currentUser stays null, identical to a genuinely logged-out
   * user. Route guards run after this completes either way, so they see
   * a consistent, already-resolved state and need no changes themselves.
   *
   * Bounded by RESTORE_SESSION_TIMEOUT_MS so a network partition or dead
   * backend can't hang app boot on a blank screen forever. A timeout is
   * treated differently from an explicit rejection: an explicit 401/400
   * from the refresh endpoint means the session really is dead, so tokens
   * are cleared; a timeout means "couldn't find out" — tokens are left in
   * storage untouched, since the session may still be perfectly valid,
   * just unreachable right now. Either way the app renders logged-out for
   * *this* boot; only an explicit rejection forces re-login on the *next*
   * one too.
   */
  restoreSession(): Observable<void> {
    if (this.#currentUser() !== null) {
      // Constructor already found a still-valid access token. Nothing to do —
      // this branch exists so restoreSession() is safe to call unconditionally.
      return of(undefined);
    }

    const accessToken = this.getAccessToken();
    const refreshToken = this.getRefreshToken();
    if (!accessToken || !refreshToken) {
      return of(undefined); // genuinely logged out, not just expired — no refresh to attempt
    }

    let userId: number;
    try {
      userId = this.decodeToken(accessToken).userId;
    } catch {
      this.clearTokens(); // corrupted/unparseable token — nothing worth restoring
      return of(undefined);
    }

    return this.refreshAccessToken(userId).pipe(
      timeout(RESTORE_SESSION_TIMEOUT_MS),
      map(() => undefined),
      catchError((error) => {
        if (!(error instanceof TimeoutError)) {
          // Backend explicitly rejected the refresh (dead/revoked token) — the
          // session really is over, so don't leave stale tokens sitting around.
          this.clearTokens();
        }
        // On timeout: leave tokens as-is. We don't know the session is dead,
        // only that we couldn't reach the backend in time — don't punish a
        // network blip with a forced re-login on the next successful boot.
        return of(undefined);
      }),
    );
  }

  /**
   * Log out the current user, clear tokens, and route to login.
   */
  logout(): void {
    this.clearTokens();
    this.#currentUser.set(null);
    this.router.navigate(['/auth/login']);
  }

  checkEmailExists(email: string): Observable<boolean> {
    return this.http
      .get<EmailCheckResponse>('/auth/check-email', { params: { email } })
      .pipe(map((response) => response.exists));
  }

  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  }

  private saveTokens(access: string, refresh: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, access);
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh);
  }

  private clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  private decodeToken(token: string): JwtPayload {
    return jwtDecode<JwtPayload>(token);
  }

  private toAuthUser(payload: JwtPayload): AuthUser {
    return {
      userId: payload.userId,
      email: payload.sub,
      role: payload.role,
    };
  }

  private isExpired(payload: JwtPayload): boolean {
    return payload.exp * 1000 < Date.now();
  }

  /**
   * Seeds the currentUser signal on service construction (i.e. on page
   * load/refresh) from whatever access token is sitting in localStorage.
   * An expired token is treated as "not logged in" rather than trusted
   * at face value — jwtDecode only parses the payload, it does not
   * validate expiry or signature. (An expired-but-refreshable token is
   * handled separately, later, by restoreSession() — this method's job
   * is only ever "is there a currently-valid token right now".)
   */
  private getUserFromToken(): AuthUser | null {
    const token = this.getAccessToken();
    if (!token) return null;
    try {
      const payload = this.decodeToken(token);
      if (this.isExpired(payload)) {
        return null;
      }
      return this.toAuthUser(payload);
    } catch {
      return null;
    }
  }
}
