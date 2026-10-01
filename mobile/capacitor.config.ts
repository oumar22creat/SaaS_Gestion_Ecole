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
  appId: 'com.schoolsaas.mobile',
  appName: 'Gestion Scolaire',
  webDir: 'dist/app/browser',
  ...(surEmulateur
    ? { android: { allowMixedContent: true }, server: { androidScheme: 'http', cleartext: true } }
    : {}),
};

export default config;
