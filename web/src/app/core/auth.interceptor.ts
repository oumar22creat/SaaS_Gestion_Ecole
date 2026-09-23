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
 * Pose l'en-tête Authorization sur les appels vers notre API, et déconnecte au premier 401
 * (jeton expiré/invalide) — voir docs/ROADMAP.md Phase 1.5+ (annoncé dans le commentaire
 * historique de AuthTokenService). Pas de rafraîchissement automatique du refresh token pour
 * ce MVP : l'utilisateur est simplement renvoyé à /login, cohérent avec la durée de vie
 * courte de l'access token (15 min, voir application.yml côté backend).
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
      // pas sur un 401 d'un appel anonyme (ex. /tenants/current/branding sans sous-domaine
      // résolu, qui échoue systématiquement en dev sur localhost) : sinon ce cas anodin
      // piège même les pages publiques comme /register, en y redirigeant l'utilisateur au
      // premier chargement.
      if (isApiRequest && tokens && error instanceof HttpErrorResponse && error.status === 401) {
        const role = authTokenService.role();
        authTokenService.clear();
        router.navigateByUrl(role === 'SUPER_ADMIN' ? '/admin/login' : '/login');
      }
      // Abonnement échu : le serveur refuse tout appel métier. Sans cette redirection,
      // l'utilisateur enchaîne les messages d'erreur écran par écran sans jamais apprendre
      // que c'est son abonnement qu'il faut renouveler. La session n'est pas effacée — il
      // reste connecté, c'est l'établissement qui est fermé, pas le compte.
      if (isApiRequest && error instanceof HttpErrorResponse && isTenantSuspended(error)) {
        router.navigateByUrl('/abonnement-echu');
      }
      return throwError(() => error);
    }),
  );
};
