import { Component, inject } from '@angular/core';
import { MATERIAL_IMPORTS } from '../../../shared/material.imports';
import { DatePipe } from '@angular/common';
import { ProjectResponse } from '../../../core/models/project.model';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';

@Component({
  imports: [...MATERIAL_IMPORTS, DatePipe],
  selector: 'app-project-view-dialog',
  styleUrl: './project-view-dialog.scss',
  templateUrl: './project-view-dialog.html',
})
export class ProjectViewDialog {
  protected readonly project = inject<ProjectResponse>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ProjectViewDialog>);
  private readonly router = inject(Router);

  close(): void {
    this.dialogRef.close();
  }

  /**
   * Closes the dialog and navigates to the scoped task list, passing the
   * project title via router state so TaskList can show it immediately
   * without a second network call — falls back to just the id on a hard
   * refresh/direct URL visit, since state doesn't survive that.
   */
  viewTasks(): void {
    this.dialogRef.close();
    this.router.navigate(['/projects', this.project.id, 'tasks'], {
      state: { projectTitle: this.project.title },
    });
  }
}
