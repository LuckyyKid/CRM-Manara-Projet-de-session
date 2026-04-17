import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
<<<<<<< HEAD
import { CurrentUserModel } from '../models/current-user.model';
=======
>>>>>>> origin/main

export const authGuard: CanActivateFn = async (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const currentUser = await authService.loadSession();
  if (currentUser) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: { redirectTo: state.url },
  });
};
<<<<<<< HEAD

function requireRole(expectedRole: string): CanActivateFn {
  return async (_route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const currentUser = await authService.loadSession();
    if (!currentUser) {
      return router.createUrlTree(['/login'], {
        queryParams: { redirectTo: state.url },
      });
    }

    if (hasRole(currentUser, expectedRole)) {
      return true;
    }

    return router.createUrlTree([authService.dashboardPath()]);
  };
}

function hasRole(currentUser: CurrentUserModel, expectedRole: string): boolean {
  return currentUser.accountType === expectedRole || currentUser.user.role === expectedRole;
}

export const adminGuard = requireRole('ROLE_ADMIN');
export const parentGuard = requireRole('ROLE_PARENT');
export const animateurGuard = requireRole('ROLE_ANIMATEUR');
=======
>>>>>>> origin/main
