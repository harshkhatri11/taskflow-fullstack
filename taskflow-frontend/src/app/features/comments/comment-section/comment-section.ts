import { Component, OnInit, computed, inject, input } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { DatePipe } from '@angular/common';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { AuthService } from '../../../core/auth/auth';
import { Notification } from '../../../shared/notification';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog';
import { avatarColor } from '../../../shared/avatar-color';
import { TaskComment } from '../task-comment';
import { TaskResponse } from '../../../core/models/task.model';
import { CommentResponse } from '../../../core/models/comment.model';

@Component({
  imports: [...MATERIAL_IMPORTS, ReactiveFormsModule, DatePipe],
  selector: 'app-comment-section',
  styleUrl: './comment-section.scss',
  templateUrl: './comment-section.html',
})
export class CommentSection implements OnInit {
  readonly task = input.required<TaskResponse>();

  readonly commentService = inject(TaskComment);
  protected readonly authService = inject(AuthService);
  private readonly notificationService = inject(Notification);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);

  protected readonly avatarColor = avatarColor;

  readonly form = this.fb.group({
    content: ['', [Validators.required]],
  });

  readonly canPost = computed(() => {
    const user = this.authService.currentUser();
    if (!user) return false;
    if (user.role === 'ADMIN' || user.role === 'MANAGER') return true;
    return this.task().assignedTo.id === user.userId;
  });

  ngOnInit(): void {
    this.commentService.loadComments(this.task().id);
  }

  canDelete(comment: CommentResponse): boolean {
    const user = this.authService.currentUser();
    if (!user) return false;
    if (user.role === 'ADMIN' || user.role === 'MANAGER') return true;
    return comment.authorId !== null && comment.authorId === user.userId;
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { content } = this.form.getRawValue();

    this.commentService.createComment(this.task().id, { content: content! }).subscribe({
      next: () => {
        this.form.reset();
        this.commentService.loadComments(this.task().id);
      },
      error: () => this.notificationService.showError('Failed to post comment.'),
    });
  }

  deleteComment(comment: CommentResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '360px',
      data: {
        title: 'Delete comment',
        message: 'Delete this comment? This action cannot be undone.',
        confirmText: 'Delete',
      } satisfies ConfirmDialogData,
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.commentService.deleteComment(comment.id).subscribe({
        next: () => this.commentService.loadComments(this.task().id),
        error: () => this.notificationService.showError('Failed to delete comment.'),
      });
    });
  }
}
