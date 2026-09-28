import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { DatePipe } from '@angular/common';

import { Task } from '../task';

import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { AuthService } from '../../../core/auth/auth';
import { Notification } from '../../../shared/notification';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog';
import { TaskViewDialog } from '../task-view-dialog/task-view-dialog';
import { TaskEditDialog, TaskEditDialogData } from '../task-edit-dialog/task-edit-dialog';
import { avatarColor } from '../../../shared/avatar-color';
import { TaskStatus } from '../../../core/models/enums';
import { TaskRequest, TaskResponse } from '../../../core/models/task.model';

const STATUS_OPTIONS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

@Component({
  imports: [...MATERIAL_IMPORTS, DatePipe, RouterLink],
  selector: 'app-task-list',
  styleUrl: './task-list.scss',
  templateUrl: './task-list.html',
})
export class TaskList implements OnInit {
  protected readonly taskService = inject(Task);
  protected readonly dialog = inject(MatDialog);
  protected readonly authService = inject(AuthService);
  protected readonly notificationService = inject(Notification);
  private readonly route = inject(ActivatedRoute);

  protected readonly statusOptions = STATUS_OPTIONS;
  protected readonly avatarColor = avatarColor;

  private readonly projectId = signal(0);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  private readonly sortParam = signal<string | undefined>(undefined);

  // Populated only when arriving via ProjectViewDialog's "View Tasks" button
  // (passed through router state); absent on a direct URL visit/refresh,
  // since router state doesn't survive that.
  protected readonly projectTitle = signal<string | undefined>(
    (history.state as { projectTitle?: string } | null)?.projectTitle,
  );

  private readonly savingIds = signal<Set<number>>(new Set());

  protected readonly isEmployee = computed(
    () => this.authService.currentUser()?.role === 'EMPLOYEE',
  );

  protected readonly canManage = computed(() => {
    const role = this.authService.currentUser()?.role;
    return role === 'ADMIN' || role === 'MANAGER';
  });

  // 'actions' stays in every role's column set — View is available to all;
  // Edit/Delete inside that cell are individually gated by canManage().
  protected readonly columns = ['task', 'priority', 'status', 'dueDate', 'assignedTo', 'actions'];

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('projectId'));
      this.projectId.set(id);
      this.pageIndex.set(0);
      this.reload();
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.reload();
  }

  onSortChange(sort: Sort): void {
    this.pageIndex.set(0);
    this.sortParam.set(sort.direction ? `${sort.active},${sort.direction}` : undefined);
    this.reload();
  }

  isSaving(taskId: number): boolean {
    return this.savingIds().has(taskId);
  }

  onStatusChange(task: TaskResponse, newStatus: TaskStatus): void {
    if (newStatus === task.status) return;

    this.savingIds.update((ids) => new Set(ids).add(task.id));

    this.taskService.updateTaskStatus(task.id, newStatus).subscribe({
      complete: () => this.clearSaving(task.id),
      error: () => this.clearSaving(task.id),
    });
  }

  truncate(text: string, maxLength = 72): string {
    return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text;
  }

  viewTask(task: TaskResponse): void {
    this.dialog.open(TaskViewDialog, { width: '440px', data: task });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(TaskEditDialog, {
      width: '520px',
      autoFocus: 'first-tabbable',
      data: { task: null } satisfies TaskEditDialogData,
    });

    dialogRef.afterClosed().subscribe((request: TaskRequest | undefined) => {
      if (!request) return;

      this.taskService.createTask(this.projectId(), request).subscribe({
        next: () => this.reload(),
        error: () => this.notificationService.showError('Failed to create task.'),
      });
    });
  }

  openEditDialog(task: TaskResponse): void {
    const dialogRef = this.dialog.open(TaskEditDialog, {
      width: '520px',
      autoFocus: 'first-tabbable',
      data: { task } satisfies TaskEditDialogData,
    });

    dialogRef.afterClosed().subscribe((request: TaskRequest | undefined) => {
      if (!request) return;

      this.taskService.updateTask(task.id, request).subscribe({
        next: () => this.reload(),
        error: () => this.notificationService.showError('Failed to update task.'),
      });
    });
  }

  deleteTask(task: TaskResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '360px',
      data: {
        title: 'Delete task',
        message: `Delete "${task.title}"? This action cannot be undone.`,
        confirmText: 'Delete',
      } satisfies ConfirmDialogData,
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.taskService.deleteTask(task.id).subscribe({
        next: () => this.reload(),
        error: () => this.notificationService.showError('Failed to delete task.'),
      });
    });
  }

  private reload(): void {
    this.taskService.loadTasks(
      this.projectId(),
      this.pageIndex(),
      this.pageSize(),
      this.sortParam(),
    );
  }

  private clearSaving(taskId: number): void {
    this.savingIds.update((ids) => {
      const next = new Set(ids);
      next.delete(taskId);
      return next;
    });
  }
}
