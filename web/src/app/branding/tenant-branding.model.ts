export interface TenantBranding {
  name: string;
  logoUrl: string | null;
  primaryColor: string;
  secondaryColor: string;
}

// Branding neutre affiché tant que le branding réel du tenant n'a pas été résolu
// (aucun tenant, appel API en échec, ou premier chargement avant réponse du serveur).
// Voir docs/DESIGN.md §4, point 5 : jamais d'écran cassé.
export const DEFAULT_BRANDING: TenantBranding = {
  name: 'Gestion Scolaire',
  logoUrl: null,
  primaryColor: '#0f5c4c',
  secondaryColor: '#c9a227',
};
