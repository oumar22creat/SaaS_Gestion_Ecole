package com.schoolsaas.teacher;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.teacher.dto.TeacherRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherService(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Transactional
    public Teacher create(TeacherRequest request) {
        return teacherRepository.save(new Teacher(request.firstName(), request.lastName(), request.email(), request.phone()));
    }

    public Teacher getById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("TEACHER_NOT_FOUND", "Enseignant introuvable"));
    }

    /**
     * Liste paginée, filtrée par un terme de recherche libre quand il est fourni. Un terme
     * vide est traité comme absent : « rechercher rien » doit tout renvoyer, pas rien.
     */
    public Page<Teacher> list(Pageable pageable, String search) {
        if (search == null || search.isBlank()) {
            return list(pageable);
        }
        return teacherRepository.search(search.trim(), pageable);
    }

    public Page<Teacher> list(Pageable pageable) {
        return teacherRepository.findAll(pageable);
    }

    @Transactional
    public Teacher update(Long id, TeacherRequest request) {
        Teacher teacher = getById(id);
        teacher.setFirstName(request.firstName());
        teacher.setLastName(request.lastName());
        teacher.setEmail(request.email());
        teacher.setPhone(request.phone());
        return teacher;
    }

    @Transactional
    public void deactivate(Long id) {
        Teacher teacher = getById(id);
        teacher.setActive(false);
    }
}
