package com.schoolsaas.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByStudentNumber(String studentNumber);

    Page<Student> findAllBySchoolClassId(Long schoolClassId, Pageable pageable);
}
