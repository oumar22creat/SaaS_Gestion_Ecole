/**
 * Cible « simulateur iOS » : l'application compilée joint le backend lancé sur le poste
 * de développement.
 *
 * `localhost` et non 10.0.2.2 : contrairement à l'émulateur Android, qui tourne derrière son
 * propre réseau virtuel et doit passer par un alias pour atteindre son hôte, le simulateur
 * iOS partage la pile réseau du Mac — « localhost » y désigne donc bien la machine de
 * développement. C'est ce qui justifie une configuration distincte de celle d'Android.
 *
 * Le trafic en clair vers cette adresse est autorisé par Info-Debug.plist, qui n'entre que
 * dans les compilations de débogage ; l'application distribuée reste en HTTPS strict.
 */
export const environment = {
  production: false,
  envName: 'simulateur-ios',
  apiUrl: 'http://localhost:8080/api/v1',
  /** Numéro WhatsApp de l'éditeur, au format international sans « + » ni espaces (wa.me). */
  supportWhatsApp: '22379827979',
};
