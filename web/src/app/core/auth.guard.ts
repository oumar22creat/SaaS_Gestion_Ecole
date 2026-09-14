import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthTokenService } from '../auth/auth-token.service';

export const authGuard: CanActivateFn = () => {
  const authTokenService = inject(AuthTokenService);
  if (authTokenService.isAuthenticated()) {
    return true;
  }
  return inject(Router).createUrlTree(['/login']);
};
