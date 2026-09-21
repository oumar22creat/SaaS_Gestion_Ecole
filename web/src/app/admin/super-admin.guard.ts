import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';

export const superAdminGuard: CanActivateFn = () => {
  const authTokenService = inject(AuthTokenService);
  if (authTokenService.isAuthenticated() && authTokenService.role() === 'SUPER_ADMIN') {
    return true;
  }
  return inject(Router).createUrlTree(['/admin/login']);
};
