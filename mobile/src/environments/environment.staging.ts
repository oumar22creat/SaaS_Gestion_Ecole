export const environment = {
  production: false,
  envName: 'staging',
  // TODO : remplacer par le vrai domaine une fois l'hébergement staging choisi
  // (point ouvert dans docs/ARCHITECTURE.md). Contrairement au Web, une appli mobile
  // compilée ne peut pas utiliser une URL relative : il faut une URL absolue.
  apiUrl: 'https://api-staging.schoolsaas.example/api/v1',
  /** Numéro WhatsApp de l'éditeur, au format international sans « + » ni espaces (wa.me). */
  supportWhatsApp: '22379827979',
};
