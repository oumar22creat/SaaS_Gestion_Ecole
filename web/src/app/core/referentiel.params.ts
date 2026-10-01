/**
 * Paramètres des appels qui chargent un référentiel entier (classes, matières, salles,
 * enseignants) plutôt qu'une page d'un tableau.
 *
 * Le nom du paramètre n'est pas libre : l'API pagine avec le `Pageable` de Spring, qui lit
 * `size` — c'est d'ailleurs ce que produit déjà `pageParams` pour les écrans paginés. Un
 * `pageSize` est accepté sans erreur et simplement ignoré, et la réponse revient à la taille
 * par défaut, vingt lignes. Les listes déroulantes et les sélecteurs s'en trouvaient
 * tronqués sans que rien ne le signale.
 */
export const PARAMS_REFERENTIEL = { size: 200 } as const;
