package com.schoolsaas.common;

/** Enveloppe de réponse succès uniforme — voir docs/API_CONVENTIONS.md. */
public record ApiResponse<T>(T data, PageMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> of(T data, PageMeta meta) {
        return new ApiResponse<>(data, meta);
    }

    /**
     * @param total   nombre d'éléments correspondant au filtre, toutes pages confondues
     * @param totalSum somme d'un montant sur l'ensemble du filtre, en centimes, ou null
     *                 quand la liste n'en porte pas.
     *
     *                 <p>Elle voyage avec la page parce qu'un écran comptable affiche un
     *                 total. Sans elle, il ne peut que sommer les lignes reçues — et une
     *                 liste paginée lui ferait afficher le total de vingt lignes sur deux
     *                 cents, présenté comme le total de la période. Un chiffre faux sur un
     *                 écran de trésorerie est pire qu'un tableau long : c'est le genre
     *                 d'erreur qu'on ne découvre qu'au moment de justifier une caisse.
     */
    public record PageMeta(int page, int pageSize, long total, Long totalSum) {

        public PageMeta(int page, int pageSize, long total) {
            this(page, pageSize, total, null);
        }
    }
}
