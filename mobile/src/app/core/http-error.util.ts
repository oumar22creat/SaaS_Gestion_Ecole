import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody } from './api-response.model';

/** Identique à web/src/app/core/http-error.util.ts — centralise l'extraction du message d'erreur pour tous les écrans. */
export function extractErrorMessage(
  error: unknown,
  fallback = 'Une erreur est survenue. Merci de réessayer.',
): string {
  if (error instanceof HttpErrorResponse) {
    // status 0 : la requête n'a jamais atteint le serveur (réseau coupé, serveur arrêté,
    // origine refusée par CORS). Le message de repli de l'appelant — « Identifiants
    // invalides » sur l'écran de connexion — serait alors faux : rien n'a été vérifié.
    // Sur mobile, en zone mal couverte, c'est le cas le plus fréquent des deux.
    if (error.status === 0) {
      return 'Impossible de joindre le serveur. Vérifiez votre connexion, puis réessayez.';
    }
    const body = error.error as ApiErrorBody;
    if (body?.error?.message) {
      return body.error.message;
    }
  }
  return fallback;
}
