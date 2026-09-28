import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { Notification } from '../../shared/notification';
import { Task } from '../tasks/task';

export const projectAccessGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const taskService = inject(Task);
  const notify = inject(Notification);

  const projectId = route.paramMap.get('projectId');

  if (!projectId) {
    router.navigate(['/projects']);
    return false;
  }

  return taskService.checkProjectAccess(Number(projectId)).pipe(
    catchError((error) => {
      const message = error.error?.message ?? 'You do not have permission to view this project.';
      notify.showError(message);
      router.navigate(['/projects']);
      return of(false);
    }),
  );
};
