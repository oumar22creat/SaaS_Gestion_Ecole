import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { TokenRefreshService } from '../auth/token-refresh.service';
import { PushNotificationService } from '../push/push-notification.service';
import { ApiErrorBody } from './api-response.model';

/** Le code métier, pas le seul statut : un 403 ordinaire reste un défaut de droits. */
function isTenantSuspended(error: HttpErrorResponse): boolean {
  return error.status === 403 && (error.error as ApiErrorBody)?.error?.code === 'TENANT_SUSPENDED';
}

/** Le rafraîchissement lui-même ne doit pas déclencher un rafraîchissement : boucle infinie. */
function isRefreshCall(req: HttpRequest<unknown>): boolean {
  return req.url.endsWith('/auth/refresh');
}

/**
 * Identique à web/src/app/core/auth.interceptor.ts : pose l'en-tête Authorization et renouvelle
 * silencieusement la session à l'expiration du jeton d'accès.
 *
 * <p>L'enjeu est plus fort ici qu'ailleurs : un enseignant qui fait l'appel se retrouvait
 * renvoyé à l'écran de connexion au bout d'un quart d'heure, devant sa classe.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authTokenService = inject(AuthTokenService);
  const tokenRefreshService = inject(TokenRefreshService);
  const router = inject(Router);
  const injector = inject(Injector);

  const isApiRequest = req.url.startsWith(environment.apiUrl);
  const tokens = authTokenService.read();
  const authorizedReq =
    isApiRequest && tokens
      ? req.clone({ setHeaders: { Authorization: `Bearer ${tokens.accessToken}` } })
      : req;

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      // Ne tente rien si on n'avait pas envoyé de jeton : un 401 sur un appel anonyme (ex.
      // /tenants/current/branding sans tenant résolu) ne dit rien de la session.
      if (
        isApiRequest &&
        tokens &&
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isRefreshCall(req)
      ) {
        return from(tokenRefreshService.refresh()).pipe(
          switchMap((renouveles) =>
            next(req.clone({ setHeaders: { Authorization: `Bearer ${renouveles.accessToken}` } })),
          ),
          catchError(() => {
            /*
             * Session perdue : plus aucune requête authentifiée ne peut retirer l'appareil du
             * compte. On invalide au moins son jeton chez Firebase, pour que les
             * notifications de ce compte cessent d'arriver sur ce téléphone. Résolu à la
             * demande : le service push dépend lui-même de HttpClient.
             */
            void injector.get(PushNotificationService).invaliderLocalement();
            authTokenService.clear();
            void router.navigateByUrl('/login');
            return throwError(() => error);
          }),
        );
      }

      // Abonnement échu : le serveur ferme tout. Un enseignant verrait sinon une erreur
      // technique en pleine feuille d'appel et conclurait à une panne de l'application.
      if (isApiRequest && error instanceof HttpErrorResponse && isTenantSuspended(error)) {
        void router.navigateByUrl('/abonnement-echu');
      }
      return throwError(() => error);
    }),
  );
};
