import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody } from './api-response.model';

/** Même logique d'extraction que RegistrationPage à l'origine, centralisée pour tous les écrans. */
export function extractErrorMessage(error: unknown, fallback = 'Une erreur est survenue. Merci de réessayer.'): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiErrorBody;
    if (body?.error?.message) {
      return body.error.message;
    }
  }
  return fallback;
}
