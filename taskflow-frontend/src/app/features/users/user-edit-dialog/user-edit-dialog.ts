import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { FormPulse } from '../../../shared/form-pulse/form-pulse';
import { Role } from '../../../core/models/enums'; // adjust path if different
import { UserRequest, UserResponse, UserUpdateRequest } from '../../../core/models/user.model';

export interface UserEditDialogData {
  user: UserResponse | null;
}

const ROLE_OPTIONS: Role[] = ['ADMIN', 'MANAGER', 'EMPLOYEE'];

@Component({
  imports: [...MATERIAL_IMPORTS, ReactiveFormsModule, FormPulse],
  selector: 'app-user-edit-dialog',
  styleUrl: './user-edit-dialog.scss',
  templateUrl: './user-edit-dialog.html',
})
export class UserEditDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  protected readonly dialogRef = inject(MatDialogRef<UserEditDialog>);
  protected readonly data = inject<UserEditDialogData>(MAT_DIALOG_DATA);

  protected readonly roleOptions = ROLE_OPTIONS;
  protected readonly isEditMode = this.data.user !== null;

  // password control always exists (keeps getRawValue()'s inferred type
  // stable) — validators are added conditionally below, and the field is
  // simply not rendered in edit mode, nor included when building the
  // UserUpdateRequest on submit.
  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    role: ['EMPLOYEE' as Role, [Validators.required]],
    password: [''],
  });

  ngOnInit(): void {
    if (!this.isEditMode) {
      this.form.controls.password.addValidators([Validators.required, Validators.minLength(6)]);
      this.form.controls.password.updateValueAndValidity();
    }

    const user = this.data.user;
    if (user) {
      this.form.patchValue({
        name: user.name,
        email: user.email,
        role: user.role,
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    if (this.isEditMode) {
      const request: UserUpdateRequest = {
        name: raw.name!,
        email: raw.email!,
        role: raw.role!,
      };
      this.dialogRef.close(request);
      return;
    }

    const request: UserRequest = {
      name: raw.name!,
      email: raw.email!,
      role: raw.role!,
      password: raw.password!,
    };
    this.dialogRef.close(request);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}
