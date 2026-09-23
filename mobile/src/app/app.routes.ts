import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { roleGuard } from './core/role.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login.page').then((m) => m.LoginPage),
  },
  {
    // Hors du parcours normal : l'application est fermée, proposer le menu n'aurait pas de sens.
    path: 'abonnement-echu',
    loadComponent: () =>
      import('./billing/subscription-blocked.page').then((m) => m.SubscriptionBlockedPage),
    canActivate: [authGuard],
  },
  {
    path: 'home',
    loadComponent: () => import('./home/home.page').then((m) => m.HomePage),
    canActivate: [authGuard],
  },
  {
    path: 'attendance',
    loadComponent: () => import('./attendance/roll-call.page').then((m) => m.RollCallPage),
    canActivate: [authGuard, roleGuard],
  },
  {
    path: 'exams',
    loadComponent: () => import('./grade/exam-list.page').then((m) => m.ExamListPage),
    canActivate: [authGuard, roleGuard],
  },
  {
    path: 'exams/:examId/grades',
    loadComponent: () => import('./grade/grade-entry.page').then((m) => m.GradeEntryPage),
    canActivate: [authGuard, roleGuard],
  },
  {
    path: 'timetable',
    loadComponent: () => import('./timetable/timetable.page').then((m) => m.TimetablePage),
    canActivate: [authGuard, roleGuard],
  },
  {
    path: 'portal',
    loadComponent: () => import('./portal/children.page').then((m) => m.PortalChildrenPage),
    canActivate: [authGuard, roleGuard],
  },
  {
    path: 'portal/:studentId',
    loadComponent: () =>
      import('./portal/student-record.page').then((m) => m.PortalStudentRecordPage),
    canActivate: [authGuard, roleGuard],
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
];
