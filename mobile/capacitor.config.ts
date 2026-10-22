import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Vérification sur émulateur : CAP_EMULATEUR=1 avant `npx cap sync android`.
 *
 * Par défaut, le WebView sert l'application en « https://localhost ». Un backend de
 * développement écoutant en clair sur la machine hôte (http://10.0.2.2:8080) est alors
 * refusé comme contenu mixte, avant même d'atteindre le réseau. Basculer l'origine en
 * « http » rend l'appel homogène et le débloque.
 *
 * Ce basculement ne vaut que pour la compilation de vérification locale : sans la variable,
 * l'application conserve le schéma « https », qui est le réglage recommandé et le seul
 * utilisé pour les binaires distribués.
 */
const surEmulateur = process.env['CAP_EMULATEUR'] === '1';

const config: CapacitorConfig = {
  /*
   * appId reste « com.schoolsaas.mobile » : c'est le bundle de l'application iOS déjà
   * publiée sur l'App Store, et ce champ est commun aux deux plateformes.
   *
   * Android utilise un autre identifiant (tech.schoolmanager.app, l'ancien étant pris sur
   * Google Play), fixé par applicationId dans android/app/build.gradle. Capacitor ne
   * recopie appId dans les projets natifs qu'au `cap add` ; `cap sync` ne touche ni
   * l'applicationId Android ni le PRODUCT_BUNDLE_IDENTIFIER iOS. Modifier ce champ ne
   * renommerait donc pas l'application Android, mais risquerait de désaligner iOS
   * (`cap build ios` s'en sert pour la signature manuelle).
   */
  appId: 'com.schoolsaas.mobile',
  appName: 'School Manager',
  webDir: 'dist/app/browser',
  ...(surEmulateur
    ? { android: { allowMixedContent: true }, server: { androidScheme: 'http', cleartext: true } }
    : {}),
};

export default config;
