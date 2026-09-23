export interface Student {
  id: number;
  studentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  gender: string | null;
  schoolClassId: number | null;
  active: boolean;
  /**
   * Adresse de connexion au portail mobile, ou null si aucun accès n'est ouvert. Sa présence
   * tient lieu d'indicateur d'accès et porte l'information que le secrétariat dicte à une
   * famille qui a perdu son mot de passe.
   */
  portalEmail: string | null;
}

export interface StudentRequest {
  studentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  gender: string | null;
  schoolClassId: number | null;
}

export interface StudentImportResult {
  imported: number;
  errors: { line: number; message: string }[];
}

/**
 * Valeurs du champ `gender`. La colonne est du texte libre en base : la liste n'est donc
 * qu'une contrainte de saisie côté écran, et un enregistrement importé par CSV peut porter
 * autre chose — voir `genderOptions()` dans le formulaire, qui conserve une valeur inconnue
 * au lieu de l'effacer silencieusement à la première modification.
 */
export const GENDERS: { value: string; label: string }[] = [
  { value: 'F', label: 'Féminin' },
  { value: 'M', label: 'Masculin' },
];
