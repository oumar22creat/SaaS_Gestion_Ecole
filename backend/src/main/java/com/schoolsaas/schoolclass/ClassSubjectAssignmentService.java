package com.schoolsaas.schoolclass;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.dto.ClassSubjectAssignmentRequest;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.TeacherRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Affectation enseignant/classe/matière (cahier-des-charges.md §8, ROADMAP.md 1.5). */
@Service
public class ClassSubjectAssignmentService {

    private final ClassSubjectAssignmentRepository assignmentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public ClassSubjectAssignmentService(
            ClassSubjectAssignmentRepository assignmentRepository,
            SchoolClassRepository schoolClassRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository) {
        this.assignmentRepository = assignmentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional
    public ClassSubjectAssignment assign(Long classId, ClassSubjectAssignmentRequest request) {
        if (schoolClassRepository.findById(classId).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        if (subjectRepository.findById(request.subjectId()).isEmpty()) {
            throw ApiException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable");
        }
        if (teacherRepository.findById(request.teacherId()).isEmpty()) {
            throw ApiException.notFound("TEACHER_NOT_FOUND", "Enseignant introuvable");
        }

        return assignmentRepository.findByClassIdAndSubjectId(classId, request.subjectId())
                .map(existing -> {
                    existing.setTeacherId(request.teacherId());
                    return existing;
                })
                .orElseGet(() -> assignmentRepository.save(
                        new ClassSubjectAssignment(classId, request.subjectId(), request.teacherId())));
    }

    public List<ClassSubjectAssignment> listForClass(Long classId) {
        return assignmentRepository.findAllByClassId(classId);
    }

    @Transactional
    public void unassign(Long classId, Long subjectId) {
        ClassSubjectAssignment assignment = assignmentRepository.findByClassIdAndSubjectId(classId, subjectId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND", "Affectation introuvable"));
        assignmentRepository.delete(assignment);
    }
}
