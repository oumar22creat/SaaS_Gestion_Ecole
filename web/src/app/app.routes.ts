import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  {
    path: 'register',
    loadComponent: () => import('./registration/registration.page').then((m) => m.RegistrationPage),
  },
  {
    path: 'login',
    loadComponent: () => import('./auth/login.page').then((m) => m.LoginPage),
  },
  {
    path: '',
    loadComponent: () => import('./shell/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./statistics/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'teachers',
        loadComponent: () => import('./teacher/teacher-list.page').then((m) => m.TeacherListPage),
      },
      {
        path: 'subjects',
        loadComponent: () => import('./subject/subject-list.page').then((m) => m.SubjectListPage),
      },
      {
        path: 'classes',
        loadComponent: () => import('./schoolclass/school-class-list.page').then((m) => m.SchoolClassListPage),
      },
      {
        path: 'classes/:classId/subjects',
        loadComponent: () =>
          import('./schoolclass/class-subject-assignment.page').then((m) => m.ClassSubjectAssignmentPage),
      },
      {
        path: 'students',
        loadComponent: () => import('./student/student-list.page').then((m) => m.StudentListPage),
      },
      {
        path: 'students/import',
        loadComponent: () => import('./student/student-import.page').then((m) => m.StudentImportPage),
      },
      {
        path: 'parents',
        loadComponent: () => import('./parent/parent-list.page').then((m) => m.ParentListPage),
      },
      {
        path: 'parents/:parentId/students',
        loadComponent: () => import('./parent/student-parent-link.page').then((m) => m.StudentParentLinkPage),
      },
      {
        path: 'timetable',
        loadComponent: () => import('./timetable/timetable-list.page').then((m) => m.TimetableListPage),
      },
      {
        path: 'rooms',
        loadComponent: () => import('./timetable/room-list.page').then((m) => m.RoomListPage),
      },
      {
        path: 'attendance',
        loadComponent: () => import('./attendance/roll-call.page').then((m) => m.RollCallPage),
      },
      {
        path: 'attendance/:recordId/history',
        loadComponent: () => import('./attendance/attendance-history.page').then((m) => m.AttendanceHistoryPage),
      },
      {
        path: 'exams',
        loadComponent: () => import('./grade/exam-list.page').then((m) => m.ExamListPage),
      },
      {
        path: 'exams/:examId/grades',
        loadComponent: () => import('./grade/grade-entry.page').then((m) => m.GradeEntryPage),
      },
    ],
  },
];
