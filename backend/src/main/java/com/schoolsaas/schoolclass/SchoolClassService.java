package com.schoolsaas.schoolclass;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.dto.SchoolClassRequest;
import com.schoolsaas.teacher.TeacherRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository, TeacherRepository teacherRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.teacherRepository = teacherRepository;
    }

    @Transactional
    public SchoolClass create(SchoolClassRequest request) {
        validateHeadTeacher(request.headTeacherId());
        return schoolClassRepository.save(new SchoolClass(request.name(), request.headTeacherId()));
    }

    public SchoolClass getById(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable"));
    }

    public Page<SchoolClass> list(Pageable pageable) {
        return schoolClassRepository.findAll(pageable);
    }

    @Transactional
    public SchoolClass update(Long id, SchoolClassRequest request) {
        validateHeadTeacher(request.headTeacherId());
        SchoolClass schoolClass = getById(id);
        schoolClass.setName(request.name());
        schoolClass.setHeadTeacherId(request.headTeacherId());
        return schoolClass;
    }

    @Transactional
    public void delete(Long id) {
        schoolClassRepository.delete(getById(id));
    }

    private void validateHeadTeacher(Long headTeacherId) {
        if (headTeacherId != null && teacherRepository.findById(headTeacherId).isEmpty()) {
            throw ApiException.notFound("TEACHER_NOT_FOUND", "Enseignant introuvable");
        }
    }
}
