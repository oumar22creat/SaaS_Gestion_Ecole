import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';
import { firstPathForRole } from '../shell/nav-links';

/** Empêche d'ouvrir une URL staff dont le rôle n'est pas dans `route.data.roles`. */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authTokenService = inject(AuthTokenService);
  const allowed = (route.data['roles'] as string[] | undefined) ?? [];
  const role = authTokenService.role();
  if (role && allowed.includes(role)) {
    return true;
  }
  return inject(Router).createUrlTree([firstPathForRole(role)]);
};
