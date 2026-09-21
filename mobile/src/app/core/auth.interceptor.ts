import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';

/**
 * Identique à web/src/app/core/auth.interceptor.ts : pose l'en-tête Authorization sur les
 * appels vers notre API, et déconnecte au premier 401 (jeton expiré/invalide) reçu pour un
 * appel authentifié — pas de rafraîchissement automatique du refresh token pour ce MVP.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authTokenService = inject(AuthTokenService);
  const router = inject(Router);

  const isApiRequest = req.url.startsWith(environment.apiUrl);
  const tokens = authTokenService.read();
  const authorizedReq =
    isApiRequest && tokens
      ? req.clone({ setHeaders: { Authorization: `Bearer ${tokens.accessToken}` } })
      : req;

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      // Ne déconnecte que si on avait effectivement envoyé un jeton rejeté par le serveur —
      // pas sur un 401 d'un appel anonyme (ex. /tenants/current/branding sans tenant résolu).
      if (isApiRequest && tokens && error instanceof HttpErrorResponse && error.status === 401) {
        authTokenService.clear();
        router.navigateByUrl('/login');
      }
      return throwError(() => error);
    }),
  );
};
