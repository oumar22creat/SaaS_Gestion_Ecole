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
}

export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: string[];
  };
}
