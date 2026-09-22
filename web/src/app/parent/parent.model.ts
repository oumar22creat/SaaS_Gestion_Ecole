export interface Parent {
  id: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}

export interface ParentRequest {
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
}

export interface StudentParentLink {
  studentId: number;
  parentId: number;
  relationship: string;
  primaryContact: boolean;
}

export interface StudentParentLinkRequest {
  parentId: number;
  relationship: string;
  primaryContact: boolean;
}

/**
 * Liens de parenté proposés à la saisie. Comme pour le sexe de l'élève, la colonne est du
 * texte libre en base : une valeur déjà enregistrée hors de cette liste doit rester
 * modifiable sans être perdue.
 */
export const PARENT_RELATIONSHIPS: { value: string; label: string }[] = [
  { value: 'MERE', label: 'Mère' },
  { value: 'PERE', label: 'Père' },
  { value: 'TUTEUR', label: 'Tuteur / tutrice' },
];
