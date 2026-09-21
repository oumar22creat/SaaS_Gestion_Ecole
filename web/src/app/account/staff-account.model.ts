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
 * Rôles attribuables depuis un établissement. Élève et Parent en sont absents : aucun
 * portail ne leur est construit (ADR-010), le backend refuse d'ailleurs ces rôles.
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

export function roleLabelFor(role: string): string {
  return ASSIGNABLE_ROLES.find((entry) => entry.value === role)?.label ?? role;
}
