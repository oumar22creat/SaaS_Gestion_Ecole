export const environment = {
  production: false,
  envName: 'staging',
  // URL relative : le reverse proxy Nginx (voir CLAUDE.md/ARCHITECTURE.md) route /api/v1
  // vers le backend sur le même domaine. Hébergement staging non encore choisi (point
  // ouvert dans docs/ARCHITECTURE.md).
  apiUrl: '/api/v1',
};
