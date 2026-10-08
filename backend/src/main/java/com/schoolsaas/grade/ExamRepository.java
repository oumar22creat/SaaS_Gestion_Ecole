package com.schoolsaas.grade;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findAllBySubjectId(Long subjectId);

    List<Exam> findAllBySchoolClassIdAndSubjectId(Long schoolClassId, Long subjectId);

    List<Exam> findAllBySchoolClassId(Long schoolClassId);

    /**
     * Les évaluations d'une classe dans une matière, bornées à une période et dans l'ordre où
     * elles ont eu lieu. C'est ce que demande un bulletin : « devoir 1 » est le premier devoir
     * du trimestre, pas le premier jamais saisi.
     */
    List<Exam> findAllBySchoolClassIdAndSubjectIdAndExamDateBetweenOrderByExamDateAsc(
            Long schoolClassId, Long subjectId, LocalDate from, LocalDate to);
}
