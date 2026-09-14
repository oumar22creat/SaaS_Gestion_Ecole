export interface NavLink {
  label: string;
  path: string;
  /** Rôles autorisés côté serveur (@PreAuthorize) — filtre l'affichage, pas la sécurité (voir jwt.util.ts). */
  roles: string[];
}

export const NAV_LINKS: NavLink[] = [
  { label: 'Tableau de bord', path: '/dashboard', roles: ['ADMIN', 'DIRECTION'] },
  { label: 'Élèves', path: '/students', roles: ['ADMIN', 'DIRECTION', 'SECRETARY'] },
  { label: 'Parents', path: '/parents', roles: ['ADMIN', 'DIRECTION', 'SECRETARY'] },
  { label: 'Enseignants', path: '/teachers', roles: ['ADMIN', 'DIRECTION'] },
  { label: 'Classes', path: '/classes', roles: ['ADMIN', 'DIRECTION'] },
  { label: 'Matières', path: '/subjects', roles: ['ADMIN', 'DIRECTION'] },
  { label: 'Emploi du temps', path: '/timetable', roles: ['ADMIN', 'DIRECTION'] },
  { label: 'Absences', path: '/attendance', roles: ['ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE'] },
  { label: 'Notes', path: '/exams', roles: ['ADMIN', 'DIRECTION', 'TEACHER'] },
];
