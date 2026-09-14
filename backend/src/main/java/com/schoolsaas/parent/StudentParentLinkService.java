package com.schoolsaas.parent;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.parent.dto.StudentParentLinkRequest;
import com.schoolsaas.student.StudentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Association élève/parent — cahier-des-charges.md §7, ROADMAP.md 1.5. */
@Service
public class StudentParentLinkService {

    private final StudentParentRepository studentParentRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;

    public StudentParentLinkService(
            StudentParentRepository studentParentRepository,
            StudentRepository studentRepository,
            ParentRepository parentRepository) {
        this.studentParentRepository = studentParentRepository;
        this.studentRepository = studentRepository;
        this.parentRepository = parentRepository;
    }

    @Transactional
    public StudentParent link(Long studentId, StudentParentLinkRequest request) {
        if (studentRepository.findById(studentId).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
        if (parentRepository.findById(request.parentId()).isEmpty()) {
            throw ApiException.notFound("PARENT_NOT_FOUND", "Parent introuvable");
        }
        if (studentParentRepository.findByStudentIdAndParentId(studentId, request.parentId()).isPresent()) {
            throw ApiException.conflict("STUDENT_PARENT_LINK_ALREADY_EXISTS", "Ce parent est déjà associé à cet élève");
        }
        return studentParentRepository.save(
                new StudentParent(studentId, request.parentId(), request.relationship(), request.primaryContact()));
    }

    public List<StudentParent> listForStudent(Long studentId) {
        return studentParentRepository.findAllByStudentId(studentId);
    }

    public List<Long> listStudentIdsForParent(Long parentId) {
        return studentParentRepository.findAllByParentId(parentId).stream().map(StudentParent::getStudentId).toList();
    }

    @Transactional
    public void unlink(Long studentId, Long parentId) {
        StudentParent link = studentParentRepository.findByStudentIdAndParentId(studentId, parentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_PARENT_LINK_NOT_FOUND", "Association introuvable"));
        studentParentRepository.delete(link);
    }
}
