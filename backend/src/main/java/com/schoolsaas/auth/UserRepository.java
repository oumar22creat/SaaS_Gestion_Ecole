package com.schoolsaas.auth;

import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // Le filtre Hibernate tenantFilter (activé par TenantContextInterceptor) restreint déjà
    // implicitement cette requête au tenant courant — pas besoin de school_id explicite ici
    // (voir com.schoolsaas.common.TenantScopedEntity).
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findAllByActiveTrue();

    /**
     * Annuaire de la messagerie (ADR-019) : comptes actifs du tenant courant (filtre
     * Hibernate + RLS), hors appelant, dont le rôle est dans {@code roles} OU dont l'id est
     * dans {@code extraIds} (familles du périmètre d'un enseignant). {@code term} vide = tout ;
     * sinon recherche insensible à la casse sur « prénom nom » et « nom prénom ». Tri fixe par
     * nom : le client ne choisit pas la colonne de tri (pas de tri sur l'e-mail).
     * {@code extraIds} ne doit jamais être vide (passer un id sentinelle, voir l'appelant).
     */
    @Query(value = """
            SELECT u FROM User u
            WHERE u.active = true
              AND u.id <> :selfId
              AND (u.role IN :roles OR u.id IN :extraIds)
              AND (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :term, '%')))
            ORDER BY LOWER(u.lastName), LOWER(u.firstName), u.id
            """,
            countQuery = """
            SELECT COUNT(u) FROM User u
            WHERE u.active = true
              AND u.id <> :selfId
              AND (u.role IN :roles OR u.id IN :extraIds)
              AND (LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(CONCAT(u.lastName, ' ', u.firstName)) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<User> findMessagingRecipients(
            @Param("selfId") Long selfId,
            @Param("roles") Collection<Role> roles,
            @Param("extraIds") Collection<Long> extraIds,
            @Param("term") String term,
            Pageable pageable);

}
