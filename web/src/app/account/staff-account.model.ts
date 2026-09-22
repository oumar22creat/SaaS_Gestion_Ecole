/** Compte de connexion d'un membre du personnel (API /api/v1/users). */
export interface StaffAccount {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  active: boolean;
}

export interface StaffAccountRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: string;
}

/**
 * Rôles attribuables depuis cet écran. Élève et Parent en sont absents : ces comptes se
 * créent depuis la fiche de l'élève ou du parent, pour rester rattachés à elle (le backend
 * refuse d'ailleurs ces rôles ici, et refuse aussi d'y convertir un compte existant).
 */
export const ASSIGNABLE_ROLES: { value: string; label: string; hint: string }[] = [
  {
    value: 'TEACHER',
    label: 'Enseignant',
    hint: 'Feuille d’appel, saisie des notes, emploi du temps',
  },
  { value: 'SECRETARY', label: 'Secrétariat', hint: 'Élèves, parents, inscriptions' },
  { value: 'VIE_SCOLAIRE', label: 'Vie scolaire', hint: 'Absences, incidents, sanctions' },
  { value: 'ACCOUNTANT', label: 'Comptabilité', hint: 'Frais de scolarité, factures, règlements' },
  { value: 'DIRECTION', label: 'Direction', hint: 'Accès complet en lecture et pilotage' },
  { value: 'ADMIN', label: 'Administration', hint: 'Accès complet, y compris les comptes' },
];

/**
 * Rôles non attribuables ici mais bien présents dans la liste des comptes : sans eux, la
 * colonne Rôle afficherait la valeur brute de l'API pour les comptes de famille.
 */
const FAMILY_ROLE_LABELS: Record<string, string> = {
  PARENT: 'Parent',
  STUDENT: 'Élève',
};

export function roleLabelFor(role: string): string {
  return (
    ASSIGNABLE_ROLES.find((entry) => entry.value === role)?.label ?? FAMILY_ROLE_LABELS[role] ?? role
  );
}

/** Un compte de famille se gère depuis la fiche de l'élève ou du parent, pas d'ici. */
export function isFamilyAccount(account: { role: string }): boolean {
  return account.role in FAMILY_ROLE_LABELS;
}
