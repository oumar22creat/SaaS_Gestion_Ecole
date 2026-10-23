package com.schoolsaas.student;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentNumber(String studentNumber);

    /**
     * Recherche libre sur nom, prénom et matricule. Le secrétariat cherche un élève par ce
     * qu'il a sous les yeux — un nom mal orthographié, un numéro partiel — pas par identifiant.
     * La comparaison est insensible à la casse ; les accents ne sont pas normalisés (« Traore »
     * ne trouve pas « Traoré »), ce qui suppose une saisie cohérente à l'inscription.
     */
    @Query("""
            SELECT s FROM Student s
            WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :term, '%'))
            """)
    Page<Student> search(@Param("term") String term, Pageable pageable);

    Page<Student> findAllBySchoolClassId(Long schoolClassId, Pageable pageable);

    /** Élèves d'un ensemble de classes (périmètre enseignant, ADR-030). */
    Page<Student> findAllBySchoolClassIdIn(Collection<Long> schoolClassIds, Pageable pageable);

    /** Même recherche libre que {@link #search}, bornée à un ensemble de classes. */
    @Query("""
            SELECT s FROM Student s
            WHERE s.schoolClassId IN :classIds
              AND (LOWER(s.firstName) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :term, '%'))
                OR LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :term, '%')))
            """)
    Page<Student> searchInClasses(
            @Param("term") String term, @Param("classIds") Collection<Long> classIds, Pageable pageable);

    List<Student> findAllBySchoolClassId(Long schoolClassId);

    long countByActiveTrue();

    /** Fiche rattachée à un compte de connexion (portail parent/élève). */
    java.util.Optional<Student> findByUserId(Long userId);
}
