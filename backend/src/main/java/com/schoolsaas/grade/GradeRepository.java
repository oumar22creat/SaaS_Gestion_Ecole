package com.schoolsaas.grade;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findAllByExamId(Long examId);

    Optional<Grade> findByExamIdAndStudentId(Long examId, Long studentId);

    List<Grade> findAllByExamIdIn(List<Long> examIds);

    List<Grade> findAllByExamIdInAndStudentId(List<Long> examIds, Long studentId);
}
