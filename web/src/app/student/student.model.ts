export interface Student {
  id: number;
  studentNumber: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  gender: string | null;
  schoolClassId: number | null;
  active: boolean;
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
