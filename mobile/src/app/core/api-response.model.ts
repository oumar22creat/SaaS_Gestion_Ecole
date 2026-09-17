// Enveloppe de réponse uniforme du backend — voir docs/API_CONVENTIONS.md. Identique à
// web/src/app/core/api-response.model.ts (pas de package partagé entre les deux workspaces
// Angular pour l'instant, voir mobile/src/theme/variables.scss pour le même principe).
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
