import { Component, inject, OnInit, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ProjectRequest, ProjectResponse } from '../../../core/models/project.model';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { ProjectStatus } from '../../../core/models/enums';
import { AuthService } from '../../../core/auth/auth';
import { User } from '../../users/user';
import { FormPulse } from '../../../shared/form-pulse/form-pulse';
import { UserSummaryResponse } from '../../../core/models/user.model';

export interface CreateProjectDialogData {
  project: ProjectResponse | null;
}

interface ProjectForm {
  title: FormControl<string>;
  description: FormControl<string>;
  status: FormControl<ProjectStatus>;
  managerId: FormControl<number | null>;
}

@Component({
  imports: [...MATERIAL_IMPORTS, ReactiveFormsModule, FormPulse],
  selector: 'app-create-project',
  styleUrl: './create-project.scss',
  templateUrl: './create-project.html',
})
export class CreateProject implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<CreateProject>);
  private readonly authService = inject(AuthService);
  private readonly userService = inject(User);
  protected readonly data = inject<CreateProjectDialogData>(MAT_DIALOG_DATA);
  protected readonly isEditMode = this.data.project !== null;
  protected readonly isAdmin = this.authService.currentUser()?.role === 'ADMIN';
  protected readonly managers = signal<UserSummaryResponse[]>([]);
  protected readonly statusOptions: ProjectStatus[] = ['ACTIVE', 'COMPLETED', 'ARCHIVED'];

  protected readonly form = new FormGroup<ProjectForm>({
    title: new FormControl(this.data.project?.title ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(100)],
    }),
    description: new FormControl(this.data.project?.description ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(500)],
    }),
    status: new FormControl<ProjectStatus>(this.data.project?.status ?? 'ACTIVE', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    managerId: new FormControl<number | null>(
      {
        value: this.data.project?.manager.id ?? null,
        disabled: !this.isAdmin,
      },
      {
        validators: this.isAdmin ? [Validators.required] : [],
      },
    ),
  });

  get title() {
    return this.form?.get('title');
  }

  get description() {
    return this.form?.get('description');
  }

  get status() {
    return this.form?.get('status');
  }

  get managerId() {
    return this.form?.get('managerId');
  }

  ngOnInit(): void {
    if (this.isAdmin) {
      this.userService.getUsers().subscribe({
        next: (users) => {
          this.managers.set(users.filter((u) => u.role === 'MANAGER'));
        },
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    const request: ProjectRequest = {
      title: raw.title,
      description: raw.description,
      status: raw.status,
      managerId: this.isAdmin ? raw.managerId : null,
    };

    this.dialogRef.close(request);
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
