export const environment = {
  production: false,
  envName: 'staging',
  // TODO : remplacer par le vrai domaine une fois l'hébergement staging choisi
  // (point ouvert dans docs/ARCHITECTURE.md). Contrairement au Web, une appli mobile
  // compilée ne peut pas utiliser une URL relative : il faut une URL absolue.
  apiUrl: 'https://api-staging.schoolsaas.example/api/v1',
};
