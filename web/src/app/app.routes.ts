import { Route, Routes } from '@angular/router';
import { superAdminGuard } from './admin/super-admin.guard';
import { authGuard } from './core/auth.guard';
import { roleGuard } from './core/role.guard';
import { tenantWebGuard } from './core/tenant-web.guard';
import { rolesForNavPath } from './shell/nav-links';

function staffPage(
  path: string,
  load: Route['loadComponent'],
  roles = rolesForNavPath(`/${path.split('/')[0]}`),
): Route {
  return {
    path,
    loadComponent: load,
    canActivate: [roleGuard],
    data: { roles },
  };
}

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
    canActivate: [authGuard, tenantWebGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      staffPage('dashboard', () =>
        import('./statistics/dashboard.page').then((m) => m.DashboardPage),
      ),
      staffPage('teachers', () =>
        import('./teacher/teacher-list.page').then((m) => m.TeacherListPage),
      ),
      staffPage('subjects', () =>
        import('./subject/subject-list.page').then((m) => m.SubjectListPage),
      ),
      staffPage('classes', () =>
        import('./schoolclass/school-class-list.page').then((m) => m.SchoolClassListPage),
      ),
      staffPage(
        'classes/:classId/subjects',
        () =>
          import('./schoolclass/class-subject-assignment.page').then(
            (m) => m.ClassSubjectAssignmentPage,
          ),
        rolesForNavPath('/classes'),
      ),
      staffPage('students', () =>
        import('./student/student-list.page').then((m) => m.StudentListPage),
      ),
      staffPage(
        'students/import',
        () => import('./student/student-import.page').then((m) => m.StudentImportPage),
        rolesForNavPath('/students'),
      ),
      staffPage('parents', () => import('./parent/parent-list.page').then((m) => m.ParentListPage)),
      staffPage(
        'parents/:parentId/students',
        () => import('./parent/student-parent-link.page').then((m) => m.StudentParentLinkPage),
        rolesForNavPath('/parents'),
      ),
      staffPage('timetable', () =>
        import('./timetable/timetable-list.page').then((m) => m.TimetableListPage),
      ),
      staffPage('rooms', () => import('./timetable/room-list.page').then((m) => m.RoomListPage), [
        'ADMIN',
        'DIRECTION',
      ]),
      staffPage('attendance', () =>
        import('./attendance/roll-call.page').then((m) => m.RollCallPage),
      ),
      staffPage(
        'attendance/:recordId/history',
        () => import('./attendance/attendance-history.page').then((m) => m.AttendanceHistoryPage),
        rolesForNavPath('/attendance'),
      ),
      staffPage('exams', () => import('./grade/exam-list.page').then((m) => m.ExamListPage)),
      staffPage(
        'exams/:examId/grades',
        () => import('./grade/grade-entry.page').then((m) => m.GradeEntryPage),
        rolesForNavPath('/exams'),
      ),
      staffPage('report-cards', () =>
        import('./reportcard/report-card.page').then((m) => m.ReportCardPage),
      ),
      staffPage('lessons', () => import('./homework/lesson.page').then((m) => m.LessonPage)),
      staffPage('documents', () => import('./document/document.page').then((m) => m.DocumentPage)),
      staffPage('messages', () =>
        import('./messaging/messaging.page').then((m) => m.MessagingPage),
      ),
      staffPage('notifications', () =>
        import('./notification/notification.page').then((m) => m.NotificationPage),
      ),
      staffPage('discipline', () =>
        import('./discipline/discipline.page').then((m) => m.DisciplinePage),
      ),
      staffPage('statistics', () =>
        import('./statistics/advanced-statistics.page').then((m) => m.AdvancedStatisticsPage),
      ),
      staffPage('school-fees', () =>
        import('./schoolfees/school-fees.page').then((m) => m.SchoolFeesPage),
      ),
      staffPage('canteen', () => import('./canteen/canteen.page').then((m) => m.CanteenPage)),
      staffPage('library', () => import('./library/library.page').then((m) => m.LibraryPage)),
      staffPage('transport', () =>
        import('./transport/transport.page').then((m) => m.TransportPage),
      ),
      staffPage('billing', () => import('./billing/billing.page').then((m) => m.BillingPage)),
      staffPage('accounts', () =>
        import('./account/staff-account-list.page').then((m) => m.StaffAccountListPage),
      ),
      staffPage('settings', () => import('./settings/settings.page').then((m) => m.SettingsPage)),
      staffPage(
        'espace-mobile',
        () => import('./auth/mobile-space.page').then((m) => m.MobileSpacePage),
        ['PARENT', 'STUDENT'],
      ),
    ],
  },
  {
    path: 'admin/login',
    loadComponent: () => import('./admin/admin-login.page').then((m) => m.AdminLoginPage),
  },
  {
    path: 'admin',
    loadComponent: () =>
      import('./admin/platform-dashboard.page').then((m) => m.PlatformDashboardPage),
    canActivate: [superAdminGuard],
  },
];
