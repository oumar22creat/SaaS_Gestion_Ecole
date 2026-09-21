package com.schoolsaas.student;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentNumber(String studentNumber);

    Page<Student> findAllBySchoolClassId(Long schoolClassId, Pageable pageable);

    List<Student> findAllBySchoolClassId(Long schoolClassId);

    long countByActiveTrue();

    /** Fiche rattachée à un compte de connexion (portail parent/élève). */
    java.util.Optional<Student> findByUserId(Long userId);
}
