package com.schoolsaas.schoolyear;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findAllBySchoolYearId(Long schoolYearId);

    List<Enrollment> findAllBySchoolYearIdAndSchoolClassId(Long schoolYearId, Long schoolClassId);

    List<Enrollment> findAllByStudentId(Long studentId);

    Optional<Enrollment> findBySchoolYearIdAndStudentId(Long schoolYearId, Long studentId);

    long countBySchoolYearId(Long schoolYearId);
}
