export const environment = {
  production: false,
  envName: 'staging',
  // URL relative : le reverse proxy Nginx (voir CLAUDE.md/ARCHITECTURE.md) route /api/v1
  // vers le backend sur le même domaine. Hébergement staging non encore choisi (point
  // ouvert dans docs/ARCHITECTURE.md).
  apiUrl: '/api/v1',
  /** Numéro WhatsApp de l'éditeur, au format international sans « + » ni espaces (wa.me). */
  supportWhatsApp: '22379827979',
};
