package com.schoolsaas.common;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Découpe en page un résultat déjà calculé en mémoire.
 *
 * <p>Dernier recours, et il faut dire pourquoi : la pagination vaut d'abord pour ce qu'elle
 * évite de charger depuis la base. Quand elle n'intervient qu'à la fin, le serveur a déjà
 * tout lu — seul le transfert vers le navigateur est allégé. C'est un gain réel mais
 * partiel, à réserver aux listes dont le filtrage ne s'exprime pas en SQL : les impayés,
 * par exemple, se définissent par la somme des règlements rapportée au montant dû, qui ne
 * se calcule pas sans joindre et agréger.
 *
 * <p>Partout où la requête peut porter la pagination, c'est au dépôt de le faire, pas ici.
 */
public final class PagedList {

    private PagedList() {
    }

    public static <T> Page<T> of(List<T> tout, Pageable pageable) {
        if (pageable.isUnpaged()) {
            return new PageImpl<>(tout, pageable, tout.size());
        }
        int debut = (int) Math.min(pageable.getOffset(), tout.size());
        int fin = Math.min(debut + pageable.getPageSize(), tout.size());
        return new PageImpl<>(tout.subList(debut, fin), pageable, tout.size());
    }

    /** Raccourci : l'enveloppe de réponse attend la liste et les métadonnées séparément. */
    public static <T> ApiResponse<List<T>> response(Page<T> page) {
        return ApiResponse.of(
                page.getContent(),
                new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    /**
     * Même chose, avec la somme d'un montant sur l'ensemble du filtre.
     *
     * <p>À utiliser dès qu'un écran affiche un total : il ne peut pas le calculer sur les
     * lignes qu'il reçoit, puisqu'il n'en reçoit qu'une page. Lui laisser faire la somme
     * reviendrait à afficher le total de vingt lignes en le présentant comme celui de la
     * période.
     */
    public static <T> ApiResponse<List<T>> response(Page<T> page, long sommeTotale) {
        return ApiResponse.of(
                page.getContent(),
                new ApiResponse.PageMeta(
                        page.getNumber() + 1, page.getSize(), page.getTotalElements(), sommeTotale));
    }
}
