import { Component, inject, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { UserCard } from '../user-card/user-card';
import { User } from '../user';
import { UserEditDialog, UserEditDialogData } from '../user-edit-dialog/user-edit-dialog';
import { UserRequest, UserResponse, UserUpdateRequest } from '../../../core/models/user.model';
import { Notification } from '../../../shared/notification';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog';

@Component({
  imports: [...MATERIAL_IMPORTS, UserCard],
  selector: 'app-user-list',
  styleUrl: './user-list.scss',
  templateUrl: './user-list.html',
})
export class UserList implements OnInit {
  protected readonly userService = inject(User);
  protected readonly skeletonRows = Array.from({ length: 6 }, (_, i) => i);

  private readonly dialog = inject(MatDialog);
  private readonly notificationService = inject(Notification);

  ngOnInit(): void {
    this.userService.loadUsers();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(UserEditDialog, {
      width: '440px',
      autoFocus: 'first-tabbable',
      data: { user: null } satisfies UserEditDialogData,
    });

    dialogRef.afterClosed().subscribe((request: UserRequest | UserUpdateRequest | undefined) => {
      if (!request) return;

      this.userService.createUser(request as UserRequest).subscribe({
        next: () => this.userService.loadUsers(),
        error: () => this.notificationService.showError('Failed to create user.'),
      });
    });
  }

  openEditDialog(user: UserResponse): void {
    const dialogRef = this.dialog.open(UserEditDialog, {
      width: '440px',
      autoFocus: 'first-tabbable',
      data: { user } satisfies UserEditDialogData,
    });

    dialogRef.afterClosed().subscribe((request: UserRequest | UserUpdateRequest | undefined) => {
      if (!request) return;

      this.userService.updateUser(user.id, request as UserUpdateRequest).subscribe({
        next: () => this.userService.loadUsers(),
        error: () => this.notificationService.showError('Failed to update user.'),
      });
    });
  }

  deleteUser(user: UserResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '360px',
      data: {
        title: 'Delete user',
        message: `Delete "${user.name}"? This action cannot be undone.`,
        confirmText: 'Delete',
      } satisfies ConfirmDialogData,
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.userService.deleteUser(user.id).subscribe({
        next: () => this.userService.loadUsers(),
        // Surfaces the backend's actual guard messages (still-owns-projects /
        // still-has-tasks-assigned) rather than a generic failure string,
        // since those are the realistic reasons this call fails.
        error: (err) =>
          this.notificationService.showError(err?.error?.message ?? 'Failed to delete user.'),
      });
    });
  }
}
