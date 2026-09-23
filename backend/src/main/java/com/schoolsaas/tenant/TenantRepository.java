package com.schoolsaas.tenant;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);

    Optional<Tenant> findByCustomDomain(String customDomain);

    long countByStatus(TenantStatus status);

    Page<Tenant> findAllByStatus(TenantStatus status, Pageable pageable);

    /*
     * Recherche de la console plateforme. Quatre combinaisons, quatre méthodes, plutôt qu'une
     * requête unique avec des « :param IS NULL » : passé à null, le paramètre d'un LOWER() n'a
     * plus de type inférable et PostgreSQL échoue sur « function lower(bytea) does not exist ».
     * Le piège ne se déclenche que quand TOUS les critères sont vides — c'est-à-dire au premier
     * affichage de l'écran.
     */
    @Query("""
            SELECT t FROM Tenant t
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(t.subdomain) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<Tenant> search(@Param("term") String term, Pageable pageable);

    @Query("""
            SELECT t FROM Tenant t
            WHERE t.status = :status
              AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :term, '%'))
                   OR LOWER(t.subdomain) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<Tenant> searchByStatus(
            @Param("term") String term, @Param("status") TenantStatus status, Pageable pageable);
}
