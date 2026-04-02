import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

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
