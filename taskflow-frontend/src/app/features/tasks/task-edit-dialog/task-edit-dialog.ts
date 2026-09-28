import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { FormPulse } from '../../../shared/form-pulse/form-pulse';
import { User } from '../../users/user';
import { futureOrPresentValidator, parseIsoDate, toIsoDateString } from '../task-date.validators';
import { UserSummaryResponse } from '../../../core/models/user.model';
import { TaskRequest, TaskResponse } from '../../../core/models/task.model';
import { TaskPriority, TaskStatus } from '../../../core/models/enums';

export interface TaskEditDialogData {
  task: TaskResponse | null;
}

const STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];
const PRIORITY_OPTIONS: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

@Component({
  imports: [...MATERIAL_IMPORTS, ReactiveFormsModule, FormPulse],
  selector: 'app-task-edit-dialog',
  styleUrl: './task-edit-dialog.scss',
  templateUrl: './task-edit-dialog.html',
})
export class TaskEditDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(User);
  protected readonly dialogRef = inject(MatDialogRef<TaskEditDialog>);
  protected readonly data = inject<TaskEditDialogData>(MAT_DIALOG_DATA);

  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly priorityOptions = PRIORITY_OPTIONS;
  protected readonly isEditMode = this.data.task !== null;
  protected readonly today = new Date();

  protected readonly employees = signal<UserSummaryResponse[]>([]);

  protected readonly form = this.fb.group({
    title: ['', [Validators.required]],
    description: ['', [Validators.required]],
    status: ['TODO' as TaskStatus, [Validators.required]],
    priority: ['MEDIUM' as TaskPriority, [Validators.required]],
    dueDate: [null as Date | null, [Validators.required, futureOrPresentValidator()]],
    assignedToId: [null as number | null, [Validators.required]],
  });

  ngOnInit(): void {
    this.userService.getUsers().subscribe((users) => {
      this.employees.set(users.filter((u) => u.role === 'EMPLOYEE'));
    });

    const task = this.data.task;
    if (task) {
      this.form.patchValue({
        title: task.title,
        description: task.description,
        status: task.status,
        priority: task.priority,
        dueDate: parseIsoDate(task.dueDate),
        assignedToId: task.assignedTo.id,
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();

    const request: TaskRequest = {
      title: raw.title!,
      description: raw.description!,
      status: raw.status!,
      priority: raw.priority!,
      assignedToId: raw.assignedToId!,
      dueDate: toIsoDateString(raw.dueDate!),
    };

    this.dialogRef.close(request);
  }

  cancel(): void {
    this.dialogRef.close(undefined);
  }
}
