package com.schoolsaas.schoolyear;

/**
 * Devenir d'un élève sur une année donnée. Les trois premiers états décrivent une scolarité
 * qui continue ; les trois derniers une sortie de l'établissement.
 */
public enum EnrollmentStatus {
    /** Inscrit et scolarisé. */
    ENROLLED,
    /** A terminé l'année, passe dans la classe supérieure. */
    PROMOTED,
    /** Redouble : repart sur la même classe l'année suivante. */
    REPEATING,
    /** Parti dans un autre établissement. */
    TRANSFERRED,
    /** A terminé son cycle. */
    GRADUATED,
    /** A quitté l'établissement en cours d'année. */
    WITHDRAWN
}
