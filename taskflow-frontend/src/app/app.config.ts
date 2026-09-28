import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { API_BASE_URL } from './core/config/api.token';
import { environment } from '../environments/environment';
import { authInterceptor } from './core/auth/auth-interceptor';
import { AuthService } from './core/auth/auth';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { loadingInterceptor } from './core/loading/loading-interceptor';
import { provideFormErrorMessages } from './shared/form-pulse/form-error-messages';
import { provideToastr } from 'ngx-toastr';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideToastr({
      timeOut: 4000,
      positionClass: 'toast-bottom-center',
      preventDuplicates: true,
      closeButton: true,
      progressBar: true,
      progressAnimation: 'decreasing',
      easing: 'ease-out',
      easeTime: 250,
      newestOnTop: true,
    }),
    provideHttpClient(withInterceptors([loadingInterceptor, authInterceptor])),
    { provide: API_BASE_URL, useValue: environment.apiUrl },
    {
      provide: MAT_FORM_FIELD_DEFAULT_OPTIONS,
      useValue: { appearance: 'outline' },
    },
    provideFormErrorMessages({
      emailTaken: (_e, label) => ` is already registered`,
      passwordMismatch: () => ' Passwords do not match',
      pastDate: () => ' must be today or a future date',
    }),
    // Attempts a silent session restore before the app renders — covers a
    // hard page reload with an expired access token but a still-valid
    // refresh token. Resolves near-instantly (no HTTP call) for every
    // other boot state; only pays a network round-trip, capped at
    // RESTORE_SESSION_TIMEOUT_MS inside restoreSession() itself, in that
    // one case. See AuthService.restoreSession() for the full rationale.
    provideAppInitializer(() => {
      const auth = inject(AuthService);
      return auth.restoreSession();
    }),
  ],
};
