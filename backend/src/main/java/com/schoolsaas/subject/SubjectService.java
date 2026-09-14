package com.schoolsaas.subject;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.subject.dto.SubjectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public Subject create(SubjectRequest request) {
        if (subjectRepository.existsByCode(request.code())) {
            throw ApiException.conflict("SUBJECT_CODE_ALREADY_USED", "Ce code matière est déjà utilisé");
        }
        return subjectRepository.save(new Subject(request.name(), request.code(), request.coefficient()));
    }

    public Subject getById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable"));
    }

    public Page<Subject> list(Pageable pageable) {
        return subjectRepository.findAll(pageable);
    }

    @Transactional
    public Subject update(Long id, SubjectRequest request) {
        Subject subject = getById(id);
        if (!subject.getCode().equals(request.code()) && subjectRepository.existsByCode(request.code())) {
            throw ApiException.conflict("SUBJECT_CODE_ALREADY_USED", "Ce code matière est déjà utilisé");
        }
        subject.setName(request.name());
        subject.setCode(request.code());
        subject.setCoefficient(request.coefficient());
        return subject;
    }

    @Transactional
    public void delete(Long id) {
        subjectRepository.delete(getById(id));
    }
}
