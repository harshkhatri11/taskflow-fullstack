import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { DatePipe } from '@angular/common';

import { MATERIAL_IMPORTS } from '../../../shared/material.imports';

import { CommentSection } from '../../comments/comment-section/comment-section';
import { FormPulse } from '../../../shared/form-pulse/form-pulse';
import { TaskResponse } from '../../../core/models/task.model';

@Component({
  imports: [...MATERIAL_IMPORTS, DatePipe, CommentSection, FormPulse],
  selector: 'app-task-view-dialog',
  styleUrl: './task-view-dialog.scss',
  templateUrl: './task-view-dialog.html',
})
export class TaskViewDialog {
  protected readonly dialogRef = inject(MatDialogRef<TaskViewDialog>);
  protected readonly task = inject<TaskResponse>(MAT_DIALOG_DATA);
}
