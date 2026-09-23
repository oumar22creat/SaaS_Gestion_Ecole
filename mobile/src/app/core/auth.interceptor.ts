import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { ApiErrorBody } from './api-response.model';

/** Le code métier, pas le seul statut : un 403 ordinaire reste un défaut de droits. */
function isTenantSuspended(error: HttpErrorResponse): boolean {
  return error.status === 403 && (error.error as ApiErrorBody)?.error?.code === 'TENANT_SUSPENDED';
}

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
      // Abonnement échu : le serveur ferme tout. Un enseignant verrait sinon une erreur
      // technique en pleine feuille d'appel et conclurait à une panne de l'application.
      if (isApiRequest && error instanceof HttpErrorResponse && isTenantSuspended(error)) {
        router.navigateByUrl('/abonnement-echu');
      }
      return throwError(() => error);
    }),
  );
};
