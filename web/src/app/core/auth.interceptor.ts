import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';
import { TokenRefreshService } from '../auth/token-refresh.service';
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
 * Pose l'en-tête Authorization sur les appels vers notre API, et renouvelle silencieusement la
 * session quand le jeton d'accès expire.
 *
 * <p>Le jeton d'accès vit quinze minutes ; le jeton de rafraîchissement, trente jours. Au
 * premier 401, on échange le second contre un nouveau jeton d'accès et on rejoue la requête :
 * l'utilisateur ne voit rien. Il n'est renvoyé à l'écran de connexion que si ce
 * rafraîchissement échoue à son tour — session révoquée, ou trente jours écoulés.
 *
 * <p>Auparavant, le premier 401 déconnectait sèchement : au bout d'un quart d'heure de
 * travail, la sauvegarde suivante renvoyait à l'écran de connexion, en pleine saisie.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authTokenService = inject(AuthTokenService);
  const tokenRefreshService = inject(TokenRefreshService);
  const router = inject(Router);

  const isApiRequest = req.url.startsWith(environment.apiUrl);
  const tokens = authTokenService.read();
  const authorizedReq =
    isApiRequest && tokens
      ? req.clone({ setHeaders: { Authorization: `Bearer ${tokens.accessToken}` } })
      : req;

  const deconnecter = () => {
    const role = authTokenService.role();
    authTokenService.clear();
    void router.navigateByUrl(role === 'SUPER_ADMIN' ? '/admin/login' : '/login');
  };

  return next(authorizedReq).pipe(
    catchError((error: unknown) => {
      // Ne tente rien si on n'avait pas envoyé de jeton — un 401 sur un appel anonyme (ex.
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
            // Le rafraîchissement a échoué : la session est bel et bien finie.
            deconnecter();
            return throwError(() => error);
          }),
        );
      }

      // Abonnement échu : le serveur refuse tout appel métier. Sans cette redirection,
      // l'utilisateur enchaîne les messages d'erreur écran par écran sans jamais apprendre
      // que c'est son abonnement qu'il faut renouveler. La session n'est pas effacée — il
      // reste connecté, c'est l'établissement qui est fermé, pas le compte.
      if (isApiRequest && error instanceof HttpErrorResponse && isTenantSuspended(error)) {
        void router.navigateByUrl('/abonnement-echu');
      }
      return throwError(() => error);
    }),
  );
};
