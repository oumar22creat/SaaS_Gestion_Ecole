export const environment = {
  production: false,
  envName: 'dev',
  // En navigateur (ionic serve) : localhost fonctionne tel quel.
  // Sur émulateur Android : remplacer par 10.0.2.2.
  // Sur appareil physique ou simulateur iOS : utiliser l'IP LAN de la machine de dev.
  apiUrl: 'http://localhost:8080/api/v1',
};
