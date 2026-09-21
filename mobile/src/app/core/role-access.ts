export interface HomeTile {
  /** Nom Ionicons enregistré dans app.config.ts (docs/DESIGN.md §9). */
  icon: string;
  label: string;
  hint: string;
  path: string | null;
}

const TEACHER_TILES: HomeTile[] = [
  {
    icon: 'checkmark-circle-outline',
    label: "Feuille d'appel",
    hint: 'Présences et retards',
    path: '/attendance',
  },
  { icon: 'create-outline', label: 'Notes', hint: 'Saisie par évaluation', path: '/exams' },
  {
    icon: 'calendar-outline',
    label: 'Emploi du temps',
    hint: 'Vos cours de la semaine',
    path: '/timetable',
  },
];

const PARENT_TILES: HomeTile[] = [
  { icon: 'people-outline', label: 'Mes enfants', hint: 'Notes et absences', path: '/portal' },
];

const STUDENT_TILES: HomeTile[] = [
  {
    icon: 'create-outline',
    label: 'Mon suivi',
    hint: 'Mes notes et mes absences',
    path: '/portal',
  },
];

const STAFF_PEDAGOGIE = ['ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE'];

export function homeTilesForRole(role: string | null): HomeTile[] {
  if (role === 'PARENT') {
    return PARENT_TILES;
  }
  if (role === 'STUDENT') {
    return STUDENT_TILES;
  }
  if (role && STAFF_PEDAGOGIE.includes(role)) {
    return TEACHER_TILES;
  }
  return [];
}

export function homeEyebrowForRole(role: string | null): string {
  if (role === 'PARENT') {
    return 'Espace parent';
  }
  if (role === 'STUDENT') {
    return 'Espace élève';
  }
  if (role === 'TEACHER') {
    return 'Espace enseignant';
  }
  return 'Espace établissement';
}

export function canAccessMobilePath(role: string | null, path: string): boolean {
  if (path === '/home' || path === '/' || path === '') {
    return role !== null;
  }
  if (path.startsWith('/attendance')) {
    return role !== null && STAFF_PEDAGOGIE.includes(role);
  }
  if (path.startsWith('/exams') || path.startsWith('/timetable')) {
    return role === 'ADMIN' || role === 'DIRECTION' || role === 'TEACHER';
  }
  // Portail famille : réservé aux comptes rattachés à une fiche élève ou parent. Le serveur
  // revérifie de toute façon à quels élèves ce compte a droit (voir PortalService côté back).
  if (path.startsWith('/portal')) {
    return role === 'PARENT' || role === 'STUDENT';
  }
  return false;
}
