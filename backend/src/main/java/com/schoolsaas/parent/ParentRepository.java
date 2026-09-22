package com.schoolsaas.parent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    /** Fiche rattachée à un compte de connexion (portail parent/élève). */
    java.util.Optional<Parent> findByUserId(Long userId);

    /**
     * Recherche libre sur nom, prénom, e-mail et téléphone. Le téléphone compte autant que le
     * nom : c'est souvent la seule chose qu'on a en main quand un parent appelle.
     */
    @Query("""
            SELECT p FROM Parent p
            WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :term, '%'))
               OR COALESCE(p.phone, '') LIKE CONCAT('%', :term, '%')
            """)
    Page<Parent> search(@Param("term") String term, Pageable pageable);
}
