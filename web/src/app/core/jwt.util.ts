export interface DecodedAccessToken {
  role?: string;
  email?: string;
  tenantId?: number;
}

/**
 * Décodage local du payload JWT, uniquement pour affichage (rôle/email dans le shell) —
 * jamais utilisé comme source de vérité pour une décision d'autorisation, qui reste
 * entièrement du ressort du serveur (@PreAuthorize, voir docs/API_CONVENTIONS.md). Un accès
 * refusé côté client ne fait qu'éviter d'afficher un lien inutile, il n'a aucune valeur de
 * sécurité.
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
