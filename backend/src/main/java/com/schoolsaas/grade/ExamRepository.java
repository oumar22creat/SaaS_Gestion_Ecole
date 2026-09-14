package com.schoolsaas.grade;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findAllBySubjectId(Long subjectId);

    List<Exam> findAllBySchoolClassIdAndSubjectId(Long schoolClassId, Long subjectId);
}
