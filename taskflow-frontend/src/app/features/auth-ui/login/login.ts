import { Component, inject, signal } from '@angular/core';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';
import { LoginRequest } from '../../../core/models/auth.model';
import { Notification } from '../../../shared/notification';

@Component({
  imports: [...MATERIAL_IMPORTS, ReactiveFormsModule, RouterLink],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly notify = inject(Notification);

  protected readonly showPassword = signal(false);

  submitting = false;

  loginForm: FormGroup = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(6)],
    }),
  });

  get email() {
    return this.loginForm.get('email');
  }
  get password() {
    return this.loginForm.get('password');
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  shouldShowError(controlName: keyof typeof this.loginForm.controls): boolean {
    const control = this.loginForm.controls[controlName];
    return control.invalid && (control.dirty || control.touched);
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const { email, password } = this.loginForm.getRawValue();
    const payload: LoginRequest = { email, password };

    this.submitting = true;
    this.authService.login(payload).subscribe({
      next: () => {
        this.router.navigate(['/projects']);
      },
      error: (err) => {
        this.submitting = false;
        const message = err?.error?.message ?? 'Invalid email or password.';
        this.notify.showError(message);
      },
    });
  }
}
