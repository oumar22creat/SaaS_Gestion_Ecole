export interface DecodedAccessToken {
  role?: string;
  email?: string;
  tenantId?: number;
}

/**
 * Décodage local du payload JWT, uniquement pour affichage (rôle/email dans l'app) — jamais
 * utilisé comme source de vérité pour une décision d'autorisation, qui reste entièrement du
 * ressort du serveur (@PreAuthorize, voir docs/API_CONVENTIONS.md). Identique à
 * web/src/app/core/jwt.util.ts.
 */
export function decodeAccessToken(token: string): DecodedAccessToken | null {
  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(normalized)) as DecodedAccessToken;
  } catch {
    return null;
  }
}
