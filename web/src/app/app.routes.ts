import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'register',
    loadComponent: () => import('./registration/registration.page').then((m) => m.RegistrationPage),
  },
];
