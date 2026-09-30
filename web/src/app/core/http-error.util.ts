import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody } from './api-response.model';

/** Même logique d'extraction que RegistrationPage à l'origine, centralisée pour tous les écrans. */
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

/**
 * Rend lisible l'erreur d'une requête en `responseType: 'blob'`.
 *
 * <p>Une telle requête reçoit AUSSI le corps des réponses en échec sous forme de Blob :
 * `extractErrorMessage` y voit un objet opaque et retombe sur son message générique.
 * L'application connaissait donc la cause exacte — « Aucune année scolaire active : créez-en
 * une et activez-la » — et affichait « Une erreur est survenue. Merci de réessayer. »
 *
 * <p>Appelée par les services de téléchargement plutôt que par chaque écran : la conversion
 * vaut pour tous les appelants, y compris ceux qu'on ajoutera.
 */
export async function withReadableBody(error: unknown): Promise<unknown> {
  if (error instanceof HttpErrorResponse && error.error instanceof Blob) {
    try {
      return new HttpErrorResponse({
        error: JSON.parse(await error.error.text()) as ApiErrorBody,
        status: error.status,
        statusText: error.statusText,
        url: error.url ?? undefined,
      });
    } catch {
      // Corps illisible ou non JSON : l'erreur d'origine reste la meilleure information.
    }
  }
  return error;
}
