// Enveloppe de réponse uniforme du backend — voir docs/API_CONVENTIONS.md. Partagé par tous
// les services HTTP (évite de redéfinir cette forme dans chaque module, voir CLAUDE.md :
// pas de duplication évitable).
export interface ApiResponse<T> {
  data: T;
  meta?: PageMeta;
}

export interface PageMeta {
  page: number;
  pageSize: number;
  total: number;
  /**
   * Somme d'un montant sur tout le filtre, en centimes, quand la liste en porte une.
   *
   * <p>Elle vient du serveur parce qu'un écran paginé ne peut pas la calculer : il ne
   * reçoit qu'une page. La sommer côté navigateur afficherait le total de vingt lignes
   * en le présentant comme celui de la période.
   */
  totalSum?: number | null;
}

/** Une page, et le total que le serveur a calculé sur l'ensemble du filtre. */
export interface PagedTotal<T> {
  items: T[];
  total: number;
  totalSum: number;
}

export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: string[];
  };
}
