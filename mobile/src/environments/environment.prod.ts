export const environment = {
  production: true,
  envName: 'production',
  // Contrairement au Web, une application compilée ne peut pas utiliser une URL relative :
  // il lui faut une adresse absolue, figée au moment de la compilation.
  //
  // Le sous-domaine applicatif convient quel que soit l'établissement : le tenant est porté
  // par le jeton après connexion, et l'écran de connexion demande le sous-domaine de l'école.
  apiUrl: 'https://app.schoolmanager.tech/api/v1',
  /** Numéro WhatsApp de l'éditeur, au format international sans « + » ni espaces (wa.me). */
  supportWhatsApp: '22379827979',
};
