package com.schoolsaas.schoolclass;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSubjectAssignmentRepository extends JpaRepository<ClassSubjectAssignment, Long> {

    List<ClassSubjectAssignment> findAllByClassId(Long classId);

    Optional<ClassSubjectAssignment> findByClassIdAndSubjectId(Long classId, Long subjectId);

    /** Affectations d'un ensemble de fiches enseignant (périmètre enseignant, voir TeacherScope). */
    List<ClassSubjectAssignment> findAllByTeacherIdIn(Collection<Long> teacherIds);
}
