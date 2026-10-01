/**
 * Paramètres des appels qui chargent un référentiel entier (classes, matières, salles,
 * élèves d'une journée) plutôt qu'une page d'un tableau.
 *
 * Le nom du paramètre n'est pas libre : l'API pagine avec le `Pageable` de Spring, qui lit
 * `size`. Un `pageSize` est accepté sans erreur et simplement ignoré — la réponse revient
 * alors à la taille par défaut, vingt lignes. C'est ce qui se passait ici : au-delà de vingt
 * élèves, la feuille d'appel et la saisie de notes en omettaient silencieusement, sans
 * message ni page suivante, et une classe de trente paraissait en compter vingt.
 */
export const PARAMS_REFERENTIEL = { size: 200 } as const;
