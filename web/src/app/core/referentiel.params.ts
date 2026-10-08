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

/**
 * Pour les écrans qui ont besoin de la liste entière et non d'une page : une grille
 * hebdomadaire se dessine d'un bloc, et un total financier se calcule sur toutes les
 * lignes, pas sur les vingt affichées.
 *
 * <p>Ce n'est pas un contournement de la pagination : c'est l'autre cas d'usage. Un écran
 * qui somme ses lignes et n'en reçoit qu'une page affiche un chiffre faux, ce qui est pire
 * qu'un tableau long. Tant qu'un total calculé côté serveur n'existe pas pour ces écrans,
 * ils demandent tout, explicitement et non par omission.
 */
export const PARAMS_LISTE_COMPLETE = { size: 500 } as const;
