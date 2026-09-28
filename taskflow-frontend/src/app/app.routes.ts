import { Routes } from '@angular/router';
import { Unauthorized } from './features/auth-ui/unauthorized/unauthorized';
import { Register } from './features/auth-ui/register/register';
import { Login } from './features/auth-ui/login/login';
import { authGuard } from './core/auth/auth-guard';
import { roleGuard } from './core/auth/role-guard';
import { Shell } from './layout/shell/shell';
import { NotFound } from './features/not-found/not-found';
import { projectAccessGuard } from './features/projects/project-access.guard';

export const routes: Routes = [
  { path: 'auth/register', component: Register, title: 'Registration' },
  { path: 'auth/login', component: Login, title: 'Login' },
  { path: 'unauthorized', component: Unauthorized, title: 'Unauthorized' },

  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      {
        path: 'projects',
        loadComponent: () =>
          import('./features/projects/project-list/project-list').then((m) => m.ProjectList),
        title: 'Projects',
      },
      {
        path: 'projects/:projectId/tasks',
        loadComponent: () => import('./features/tasks/task-list/task-list').then((m) => m.TaskList),
        title: 'Tasks',
        canActivate: [projectAccessGuard],
      },
      {
        path: 'users',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./features/users/user-list/user-list').then((m) => m.UserList),
        title: 'Users',
      },
      { path: '', redirectTo: 'projects', pathMatch: 'full' },
    ],
  },

  { path: '**', component: NotFound, title: 'Page Not Found' },
];
