import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';

/** La console Super-Admin n'entre pas dans le shell établissement. */
export const tenantWebGuard: CanActivateFn = () => {
  const authTokenService = inject(AuthTokenService);
  if (authTokenService.role() === 'SUPER_ADMIN') {
    return inject(Router).createUrlTree(['/admin']);
  }
  return true;
};
