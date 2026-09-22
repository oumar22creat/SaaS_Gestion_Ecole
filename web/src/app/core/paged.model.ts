/**
 * Résultat d'une liste paginée. `total` vient de l'en-tête `meta` du backend et porte le
 * nombre d'éléments correspondant au filtre, pas le nombre affiché : c'est ce dont a besoin
 * le paginateur pour savoir combien de pages existent.
 */
export interface Paged<T> {
  items: T[];
  total: number;
}

/** Paramètres de liste communs à tous les écrans paginés. */
export interface PageQuery {
  page: number;
  pageSize: number;
  search?: string;
}

/**
 * Traduit une PageQuery en paramètres HTTP. Le backend est en pagination Spring, dont les
 * pages commencent à zéro alors que le paginateur Material compte à partir de zéro lui aussi
 * — l'index est donc transmis tel quel, et c'est `meta.page` (qui compte à partir de 1) qui
 * est retraduit à la lecture.
 */
export function pageParams(query: PageQuery): Record<string, string> {
  const params: Record<string, string> = {
    page: String(query.page),
    size: String(query.pageSize),
  };
  if (query.search && query.search.trim() !== '') {
    params['search'] = query.search.trim();
  }
  return params;
}
