export const environment = {
  production: true,
  envName: 'production',
  // TODO : remplacer par le vrai domaine une fois l'hébergement de production choisi
  // (point ouvert dans docs/ARCHITECTURE.md). Contrairement au Web, une appli mobile
  // compilée ne peut pas utiliser une URL relative : il faut une URL absolue.
  apiUrl: 'https://api.schoolsaas.example/api/v1',
};
