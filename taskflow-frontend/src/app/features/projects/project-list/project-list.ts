import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Project } from '../project';
import { MatDialog } from '@angular/material/dialog';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { AuthService } from '../../../core/auth/auth';
import { Notification } from '../../../shared/notification';
import { ProjectRequest, ProjectResponse } from '../../../core/models/project.model';
import { CreateProject } from '../create-project/create-project';
import { PageEvent } from '@angular/material/paginator';
import { ConfirmDialog, ConfirmDialogData } from '../../../shared/confirm-dialog/confirm-dialog';
import { DatePipe } from '@angular/common';
import { ProjectViewDialog } from '../project-view-dialog/project-view-dialog';

const AVATAR_PALETTE = [
  '#5B6EF5',
  '#E0637A',
  '#2FA894',
  '#D98A3D',
  '#8C6FE0',
  '#3D9BD9',
  '#C4548F',
  '#4FA35A',
];

@Component({
  imports: [...MATERIAL_IMPORTS, DatePipe],
  selector: 'app-project-list',
  styleUrl: './project-list.scss',
  templateUrl: './project-list.html',
})
export class ProjectList implements OnInit {
  protected readonly projectService = inject(Project);
  protected readonly dialog = inject(MatDialog);
  protected readonly authService = inject(AuthService);
  protected readonly notificationService = inject(Notification);

  protected readonly columns = ['project', 'status', 'manager', 'createdAt', 'actions'];

  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);

  protected readonly pagedProjects = computed(() => {
    const start = this.pageIndex() * this.pageSize();
    return this.projectService.projects().slice(start, start + this.pageSize());
  });

  ngOnInit(): void {
    this.projectService.loadProjects();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
  }

  truncate(text: string, maxLength = 72): string {
    return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text;
  }

  avatarColor(name: string): string {
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return AVATAR_PALETTE[Math.abs(hash) % AVATAR_PALETTE.length];
  }

  canManage(project: ProjectResponse): boolean {
    const user = this.authService.currentUser();
    if (!user) return false;
    return user.role === 'ADMIN' || (user.role === 'MANAGER' && project.manager.id === user.userId);
  }

  viewProject(project: ProjectResponse): void {
    this.dialog.open(ProjectViewDialog, { width: '440px', data: project });
  }

  openDialog(existing: ProjectResponse | null = null): void {
    const dialogRef = this.dialog.open(CreateProject, {
      width: '440px',
      autoFocus: 'first-tabbable',
      data: { project: existing },
    });

    dialogRef.afterClosed().subscribe((formValue: ProjectRequest | undefined) => {
      if (!formValue) return;

      const request$ = existing
        ? this.projectService.updateProject(existing.id, formValue)
        : this.projectService.createProject(formValue);

      request$.subscribe({
        next: () => this.projectService.loadProjects(),
        error: () =>
          this.notificationService.showError(
            existing ? 'Failed to update project.' : 'Failed to create project.',
          ),
      });
    });
  }

  deleteProject(project: ProjectResponse): void {
    const dialogRef = this.dialog.open(ConfirmDialog, {
      width: '360px',
      data: {
        title: 'Delete project',
        message: `Delete "${project.title}"? This action cannot be undone.`,
        confirmText: 'Delete',
      } satisfies ConfirmDialogData,
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean | undefined) => {
      if (!confirmed) return;

      this.projectService.deleteProject(project.id).subscribe({
        next: () => this.projectService.loadProjects(),
        error: () => this.notificationService.showError('Failed to delete project.'),
      });
    });
  }
}
