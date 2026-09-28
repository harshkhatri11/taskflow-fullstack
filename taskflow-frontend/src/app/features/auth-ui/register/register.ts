import { Component, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  AsyncValidatorFn,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { catchError, map, of, switchMap, timer } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { RegisterRequest } from '../../../core/models/auth.model';
import { AuthService } from '../../../core/auth/auth';
import { Notification } from '../../../shared/notification';

export type PasswordStrength = 'weak' | 'medium' | 'strong';

function scorePassword(value: string): PasswordStrength {
  if (!value) return 'weak';

  // 1. Hard fail on absolute minimum length safety boundary
  if (value.length < 8) return 'weak';

  let score = 0;

  // 2. Individual character diversity checks
  if (/[a-z]/.test(value)) score++; // Has lowercase
  if (/[A-Z]/.test(value)) score++; // Has uppercase
  if (/\d/.test(value)) score++; // Has a number
  if (/[^A-Za-z0-9]/.test(value)) score++; // Has a special character

  // Bonus point for longer passwords (length creates exponential security)
  if (value.length >= 12) score++;

  // 3. Stricter tier matching
  if (score <= 2) return 'weak'; // e.g., only numbers and lowercase
  if (score <= 4) return 'medium'; // e.g., combination of 3-4 criteria
  return 'strong'; // e.g., hits all criteria AND is long (Score 5)
}

function syncPasswordMismatch(password: AbstractControl, confirmPassword: AbstractControl): void {
  const mismatch = !!confirmPassword.value && password.value !== confirmPassword.value;

  if (mismatch) {
    confirmPassword.setErrors({ ...confirmPassword.errors, passwordMismatch: true });
    return;
  }

  if (confirmPassword.hasError('passwordMismatch')) {
    const { passwordMismatch, ...rest } = confirmPassword.errors ?? {};
    confirmPassword.setErrors(Object.keys(rest).length ? rest : null);
  }
}

const passwordMatchValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password');
  const confirmPassword = group.get('confirmPassword');

  if (password && confirmPassword) {
    syncPasswordMismatch(password, confirmPassword);
  }

  return null;
};

@Component({
  imports: [...MATERIAL_IMPORTS, ReactiveFormsModule, RouterLink],
  selector: 'app-register',
  styleUrl: './register.scss',
  templateUrl: './register.html',
})
export class Register {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly notify = inject(Notification);

  protected readonly showPassword = signal(false);
  protected readonly showConfirmPassword = signal(false);

  submitting = false;

  private readonly emailTakenValidator: AsyncValidatorFn = (control: AbstractControl) => {
    if (!control.value) {
      return of(null);
    }

    return timer(400).pipe(
      switchMap(() => this.authService.checkEmailExists(control.value)),
      map((exists) => (exists ? { emailTaken: true } : null)),
      catchError(() => of(null)),
    );
  };

  registerForm: FormGroup = new FormGroup(
    {
      name: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(3)],
      }),
      email: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.email],
        asyncValidators: [this.emailTakenValidator],
      }),
      password: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(3)],
      }),
      confirmPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    { validators: passwordMatchValidator },
  );

  get name() {
    return this.registerForm.get('name');
  }
  get email() {
    return this.registerForm.get('email');
  }
  get password() {
    return this.registerForm.get('password');
  }
  get confirmPassword() {
    return this.registerForm.get('confirmPassword');
  }

  togglePasswordVisibility(field: 'password' | 'confirmPassword'): void {
    if (field === 'password') {
      this.showPassword.update((v) => !v);
    } else {
      this.showConfirmPassword.update((v) => !v);
    }
  }

  private passwordValue = toSignal(this.registerForm.controls['password'].valueChanges, {
    initialValue: '',
  });
  readonly passwordStrength = computed(() => scorePassword(this.passwordValue()));

  shouldShowError(controlName: keyof typeof this.registerForm.controls): boolean {
    const control = this.registerForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  onSubmit(): void {
    if (this.registerForm.invalid || this.registerForm.pending) {
      this.registerForm.markAllAsTouched();
      return;
    }

    const { name, email, password } = this.registerForm.getRawValue();
    const payload: RegisterRequest = { name, email, password };

    this.submitting = true;
    this.authService.register(payload).subscribe({
      next: () => {
        this.notify.showSuccess('Account created — you can now log in.');
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.submitting = false;
        const message = err?.error?.message ?? 'Registration failed. Please try again.';
        this.notify.showError(message);
      },
    });
  }
}
