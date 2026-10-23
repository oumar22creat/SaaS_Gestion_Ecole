package com.schoolsaas.teacher;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Page<Teacher> findAllByActiveTrue(Pageable pageable);

    long countByActiveTrue();

    /**
     * Fiches actives portant cette adresse, comparée sous forme canonique des deux côtés
     * (trim + minuscules) : c'est le rattachement compte enseignant / fiche enseignant utilisé
     * par TeacherScope (aucune colonne user_id sur `teachers`). Le paramètre doit déjà être
     * normalisé (EmailNormalizer).
     */
    @Query("SELECT t FROM Teacher t WHERE t.active = true AND LOWER(TRIM(t.email)) = :normalizedEmail")
    List<Teacher> findActiveByNormalizedEmail(@Param("normalizedEmail") String normalizedEmail);

    /** Recherche libre sur nom, prénom et e-mail. */
    @Query("""
            SELECT t FROM Teacher t
            WHERE LOWER(t.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(t.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(COALESCE(t.email, '')) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<Teacher> search(@Param("term") String term, Pageable pageable);
}
