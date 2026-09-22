package com.schoolsaas.schoolyear;

/** Cycle de vie d'une année scolaire. Une seule peut être ACTIVE par établissement. */
public enum SchoolYearStatus {
    /** Préparée : on peut y inscrire des élèves sans perturber l'année en cours. */
    PLANNED,
    /** Celle sur laquelle travaille tout le produit. */
    ACTIVE,
    /** Archivée : consultable, plus modifiable. */
    CLOSED
}
