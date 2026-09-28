import { Component, computed, input, output, signal } from '@angular/core';
import { UserResponse } from '../../../core/models/user.model';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { DatePipe } from '@angular/common';
import { avatarColor } from '../../../shared/avatar-color';
import { ClipboardModule } from '@angular/cdk/clipboard';

const ROLE_LABEL: Record<UserResponse['role'], string> = {
  ADMIN: 'Admin',
  MANAGER: 'Manager',
  EMPLOYEE: 'Employee',
};

@Component({
  imports: [...MATERIAL_IMPORTS, DatePipe, ClipboardModule],
  selector: 'app-user-card',
  styleUrl: './user-card.scss',
  templateUrl: './user-card.html',
})
export class UserCard {
  readonly user = input.required<UserResponse>();

  readonly edit = output<UserResponse>();
  readonly delete = output<UserResponse>();

  protected readonly copied = signal(false);

  protected readonly roleLabel = computed(() => ROLE_LABEL[this.user().role]);
  protected readonly initial = computed(() => this.user().name.charAt(0).toUpperCase());
  protected readonly avatarBackground = computed(() => avatarColor(this.user().name));

  protected readonly hasStat = computed(() => {
    const u = this.user();
    return u.managedProjectCount !== null || u.assignedTaskCount !== null;
  });

  onCopied(success: boolean): void {
    if (!success) return;
    this.copied.set(true);
    setTimeout(() => this.copied.set(false), 1500);
  }
}
