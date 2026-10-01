/**
 * Cible « émulateur Android » : l'application compilée joint le backend lancé sur le poste
 * de développement.
 *
 * 10.0.2.2 est l'alias par lequel l'émulateur atteint sa machine hôte — « localhost » y
 * désigne l'émulateur lui-même, où rien n'écoute. Cette configuration existe pour que la
 * vérification sur émulateur ne réclame plus de modifier environment.ts à la main, geste
 * qu'on oublie de défaire et qui finit par casser le serveur de développement du voisin.
 *
 * Le trafic en clair vers cette adresse est autorisé par la configuration réseau du jeu de
 * sources « debug » (android/app/src/debug/res/xml/network_security_config.xml) ; la
 * compilation de production reste en HTTPS strict.
 */
export const environment = {
  production: false,
  envName: 'emulator',
  apiUrl: 'http://10.0.2.2:8080/api/v1',
  /** Numéro WhatsApp de l'éditeur, au format international sans « + » ni espaces (wa.me). */
  supportWhatsApp: '22379827979',
};
