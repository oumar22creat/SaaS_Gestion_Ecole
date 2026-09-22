import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideIonicAngular } from '@ionic/angular/provide';
import { addIcons } from 'ionicons';
import {
  bookOutline,
  businessOutline,
  calculatorOutline,
  calendarOutline,
  checkmarkCircleOutline,
  closeCircleOutline,
  createOutline,
  exitOutline,
  homeOutline,
  logInOutline,
  logOutOutline,
  peopleOutline,
  saveOutline,
  schoolOutline,
  timeOutline,
} from 'ionicons/icons';
import { TenantBrandingService } from './branding/tenant-branding.service';
import { authInterceptor } from './core/auth.interceptor';
import { routes } from './app.routes';

/**
 * Jeu d'icônes de l'application mobile (docs/DESIGN.md §9). Ionicons plutôt que Material
 * Symbols : il est embarqué dans le paquet, donc disponible hors connexion, alors qu'une
 * police distante ne s'afficherait pas dans une salle de classe mal couverte.
 * Enregistré une seule fois ici ; aucun composant ne réimporte d'icône.
 */
const APP_ICONS = {
  'book-outline': bookOutline,
  'business-outline': businessOutline,
  'calculator-outline': calculatorOutline,
  'calendar-outline': calendarOutline,
  'checkmark-circle-outline': checkmarkCircleOutline,
  'close-circle-outline': closeCircleOutline,
  'create-outline': createOutline,
  'exit-outline': exitOutline,
  'home-outline': homeOutline,
  'log-in-outline': logInOutline,
  'log-out-outline': logOutOutline,
  'people-outline': peopleOutline,
  'save-outline': saveOutline,
  'school-outline': schoolOutline,
  'time-outline': timeOutline,
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideIonicAngular({}),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideAppInitializer(() => inject(TenantBrandingService).init()),
    provideAppInitializer(() => {
      addIcons(APP_ICONS);
    }),
  ],
};
