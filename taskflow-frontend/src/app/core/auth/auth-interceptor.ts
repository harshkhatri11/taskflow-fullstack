import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { API_BASE_URL } from '../config/api.token';
import { AuthService } from './auth';
import { Notification } from '../../shared/notification';

const AUTH_ENDPOINTS_EXEMPT_FROM_AUTO_LOGOUT = ['/auth/login', '/auth/register', '/auth/refresh'];

let isRefreshing = false;
const refreshedAccessToken$ = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const baseUrl = inject(API_BASE_URL);
  const auth = inject(AuthService);
  const notify = inject(Notification);
  const token = auth.getAccessToken();

  const clonedRequest = req.clone({
    url: `${baseUrl}${req.url}`,
    ...(token && { setHeaders: { Authorization: `Bearer ${token}` } }),
  });

  return next(clonedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      const isExemptEndpoint = AUTH_ENDPOINTS_EXEMPT_FROM_AUTO_LOGOUT.some((path) =>
        req.url.includes(path),
      );

      if (error.status === 401 && !isExemptEndpoint) {
        return handle401(req, next, auth, baseUrl);
      }

      if (error.status === 403) {
        const message = error.error?.message ?? 'You do not have permission to do that.';
        notify.showError(message);
      }

      return throwError(() => error);
    }),
  );
};

function handle401(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthService,
  baseUrl: string,
): Observable<any> {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshedAccessToken$.next(null);

    return auth.refreshAccessToken().pipe(
      switchMap((newAccessToken) => {
        isRefreshing = false;
        refreshedAccessToken$.next(newAccessToken);
        return next(reauth(req, baseUrl, newAccessToken));
      }),
      catchError((refreshError) => {
        isRefreshing = false;
        refreshedAccessToken$.next(null);
        auth.logout();
        return throwError(() => refreshError);
      }),
    );
  }

  // A refresh is already in flight (triggered by a sibling request). Queue this
  // one behind it instead of firing a second /auth/refresh call.
  return refreshedAccessToken$.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap((newAccessToken) => next(reauth(req, baseUrl, newAccessToken))),
  );
}

function reauth(req: HttpRequest<unknown>, baseUrl: string, token: string): HttpRequest<unknown> {
  // req here is the ORIGINAL (un-prefixed, un-auth'd) request — safe to
  // re-apply baseUrl + the new header from scratch.
  return req.clone({
    url: `${baseUrl}${req.url}`,
    setHeaders: { Authorization: `Bearer ${token}` },
  });
}
