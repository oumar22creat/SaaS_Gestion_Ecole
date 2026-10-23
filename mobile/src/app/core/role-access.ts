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

/**
 * Offerte à tous les rôles, et c'est le point : un parent qui ne peut pas couper ce qui
 * l'importune coupe tout depuis les réglages du téléphone — et une permission refusée ne
 * se redemande pas. L'établissement perd alors le canal entier faute d'un interrupteur.
 */
const NOTIFICATION_TILE: HomeTile = {
  icon: 'notifications-outline',
  label: 'Notifications',
  hint: 'Choisir ce que vous recevez',
  path: '/notifications',
};

const STAFF_PEDAGOGIE = ['ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE'];

export function homeTilesForRole(role: string | null): HomeTile[] {
  if (role === 'PARENT') {
    return [...PARENT_TILES, NOTIFICATION_TILE];
  }
  if (role === 'STUDENT') {
    return [...STUDENT_TILES, NOTIFICATION_TILE];
  }
  if (role && STAFF_PEDAGOGIE.includes(role)) {
    // Une tuile qui mène à une route refusée renvoie aussitôt à l'accueil : la vie scolaire
    // voyait « Notes » et « Emploi du temps » sans pouvoir les ouvrir (réservés côté serveur
    // à ADMIN, DIRECTION et TEACHER). On ne montre que ce que la garde laissera passer.
    const tiles = TEACHER_TILES.filter((tile) => tile.path === null || canAccessMobilePath(role, tile.path));
    return [...tiles, NOTIFICATION_TILE];
  }
  // Même sans tuile métier, le réglage des notifications reste accessible : un comptable
  // reçoit les alertes d'abonnement et doit pouvoir les couper.
  return role ? [NOTIFICATION_TILE] : [];
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
  // Chacun règle ce qu'il reçoit, quel que soit son rôle. C'est précisément le manque qui
  // faisait couper toutes les notifications depuis les réglages du téléphone — un refus
  // système ne se redemande pas, et l'établissement perdait le canal entier.
  if (path.startsWith('/notifications')) {
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
