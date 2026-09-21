export interface NavLink {
  /** Nom d'icône Material Symbols (voir docs/DESIGN.md §9). */
  icon: string;
  label: string;
  path: string;
  /** Rôles autorisés côté serveur (@PreAuthorize) — le shell masque les liens, `roleGuard` bloque l'URL. */
  roles: string[];
  section: string;
}

const ADMIN_DIR = ['ADMIN', 'DIRECTION'];
const SCOLARITE = ['ADMIN', 'DIRECTION', 'SECRETARY'];
const PEDAGOGIE_NOTES = ['ADMIN', 'DIRECTION', 'TEACHER'];
const PEDAGOGIE_APPEL = ['ADMIN', 'DIRECTION', 'TEACHER', 'VIE_SCOLAIRE'];
const FINANCE = ['ADMIN', 'DIRECTION', 'ACCOUNTANT'];
const VIE_ETABLISSEMENT = ['ADMIN', 'DIRECTION', 'SECRETARY', 'ACCOUNTANT'];
const BIBLIOTHEQUE = ['ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY'];
const STAFF = ['ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT'];

export const NAV_LINKS: NavLink[] = [
  {
    icon: 'space_dashboard',
    label: 'Tableau de bord',
    path: '/dashboard',
    roles: ADMIN_DIR,
    section: 'Vue d’ensemble',
  },
  {
    icon: 'monitoring',
    label: 'Résultats',
    path: '/statistics',
    roles: ADMIN_DIR,
    section: 'Vue d’ensemble',
  },

  { icon: 'school', label: 'Élèves', path: '/students', roles: SCOLARITE, section: 'Scolarité' },
  {
    icon: 'family_restroom',
    label: 'Parents',
    path: '/parents',
    roles: SCOLARITE,
    section: 'Scolarité',
  },
  {
    icon: 'co_present',
    label: 'Enseignants',
    path: '/teachers',
    roles: ADMIN_DIR,
    section: 'Scolarité',
  },
  { icon: 'groups', label: 'Classes', path: '/classes', roles: ADMIN_DIR, section: 'Scolarité' },
  {
    icon: 'menu_book',
    label: 'Matières',
    path: '/subjects',
    roles: ADMIN_DIR,
    section: 'Scolarité',
  },
  {
    icon: 'calendar_month',
    label: 'Emploi du temps',
    path: '/timetable',
    roles: ADMIN_DIR,
    section: 'Scolarité',
  },

  {
    icon: 'how_to_reg',
    label: 'Absences',
    path: '/attendance',
    roles: PEDAGOGIE_APPEL,
    section: 'Pédagogie',
  },
  {
    icon: 'edit_note',
    label: 'Notes',
    path: '/exams',
    roles: PEDAGOGIE_NOTES,
    section: 'Pédagogie',
  },
  {
    icon: 'description',
    label: 'Bulletins',
    path: '/report-cards',
    roles: PEDAGOGIE_NOTES,
    section: 'Pédagogie',
  },
  {
    icon: 'history_edu',
    label: 'Cahier de textes',
    path: '/lessons',
    roles: PEDAGOGIE_NOTES,
    section: 'Pédagogie',
  },
  {
    icon: 'folder_open',
    label: 'Documents',
    path: '/documents',
    roles: STAFF,
    section: 'Pédagogie',
  },

  { icon: 'forum', label: 'Messagerie', path: '/messages', roles: STAFF, section: 'Communication' },
  {
    icon: 'notifications',
    label: 'Notifications',
    path: '/notifications',
    roles: STAFF,
    section: 'Communication',
  },

  {
    icon: 'gavel',
    label: 'Vie scolaire',
    path: '/discipline',
    roles: PEDAGOGIE_APPEL,
    section: 'Établissement',
  },
  {
    icon: 'payments',
    label: 'Frais scolaires',
    path: '/school-fees',
    roles: FINANCE,
    section: 'Établissement',
  },
  {
    icon: 'restaurant',
    label: 'Cantine',
    path: '/canteen',
    roles: VIE_ETABLISSEMENT,
    section: 'Établissement',
  },
  {
    icon: 'local_library',
    label: 'Bibliothèque',
    path: '/library',
    roles: BIBLIOTHEQUE,
    section: 'Établissement',
  },
  {
    icon: 'directions_bus',
    label: 'Transport',
    path: '/transport',
    roles: VIE_ETABLISSEMENT,
    section: 'Établissement',
  },

  {
    icon: 'badge',
    label: 'Comptes',
    path: '/accounts',
    roles: ADMIN_DIR,
    section: 'Administration',
  },
  {
    icon: 'credit_card',
    label: 'Abonnement',
    path: '/billing',
    roles: FINANCE,
    section: 'Administration',
  },
  {
    icon: 'palette',
    label: 'Établissement',
    path: '/settings',
    roles: ADMIN_DIR,
    section: 'Administration',
  },
];

export function rolesForNavPath(path: string): string[] {
  const exact = NAV_LINKS.find((link) => link.path === path);
  if (exact) {
    return exact.roles;
  }
  const prefix = NAV_LINKS.find((link) => path === link.path || path.startsWith(`${link.path}/`));
  return prefix?.roles ?? [];
}

/** Libellés affichés dans la barre latérale (le rôle technique vient du claim JWT). */
const ROLE_LABELS: Record<string, string> = {
  ADMIN: 'Administration',
  DIRECTION: 'Direction',
  TEACHER: 'Enseignant',
  SECRETARY: 'Secrétariat',
  VIE_SCOLAIRE: 'Vie scolaire',
  ACCOUNTANT: 'Comptabilité',
  SUPER_ADMIN: 'Super-Admin',
  PARENT: 'Parent',
  STUDENT: 'Élève',
};

export function roleLabel(role: string | null): string {
  return (role && ROLE_LABELS[role]) || 'Personnel';
}

const ROLE_HOME: Record<string, string> = {
  ADMIN: '/dashboard',
  DIRECTION: '/dashboard',
  TEACHER: '/attendance',
  SECRETARY: '/students',
  VIE_SCOLAIRE: '/attendance',
  ACCOUNTANT: '/school-fees',
  SUPER_ADMIN: '/admin',
  PARENT: '/espace-mobile',
  STUDENT: '/espace-mobile',
};

export function firstPathForRole(role: string | null): string {
  if (role && ROLE_HOME[role]) {
    return ROLE_HOME[role];
  }
  const match = NAV_LINKS.find((link) => role !== null && link.roles.includes(role));
  return match?.path ?? '/dashboard';
}

export function navSectionsForRole(role: string | null): { title: string; links: NavLink[] }[] {
  const links = NAV_LINKS.filter((link) => role !== null && link.roles.includes(role));
  const sections: { title: string; links: NavLink[] }[] = [];
  for (const link of links) {
    const current = sections.find((section) => section.title === link.section);
    if (current) {
      current.links.push(link);
    } else {
      sections.push({ title: link.section, links: [link] });
    }
  }
  return sections;
}
