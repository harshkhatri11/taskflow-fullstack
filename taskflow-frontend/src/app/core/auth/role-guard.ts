import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth';
import { inject } from '@angular/core';
import { AuthUser } from '../models/auth.model';

type Role = AuthUser['role'];
export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const allowedRoles = route.data['roles'] as Role[] | undefined;
  const user = auth.currentUser();

  if (!allowedRoles || allowedRoles.length === 0) {
    return true;
  }

  if (user && allowedRoles.includes(user.role)) {
    return true;
  }
  return router.createUrlTree(['/unauthorized']);
};
