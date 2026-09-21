import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { canAccessMobilePath } from './role-access';

/** Bloque les écrans staff (appel, notes) pour un parent ou un élève. */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const role = inject(AuthTokenService).role();
  const path = `/${route.routeConfig?.path ?? ''}`;
  if (canAccessMobilePath(role, path)) {
    return true;
  }
  return inject(Router).createUrlTree(['/home']);
};
