package com.schoolsaas.grade;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.grade.dto.ExamRequest;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.SubjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD évaluations — cahier-des-charges.md §11, ROADMAP.md 1.8. */
@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    public ExamService(ExamRepository examRepository, SchoolClassRepository schoolClassRepository, SubjectRepository subjectRepository) {
        this.examRepository = examRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional
    public Exam create(ExamRequest request) {
        validateReferences(request);
        return examRepository.save(new Exam(
                request.schoolClassId(), request.subjectId(), request.label(), request.maxScore(), request.coefficient(),
                request.examDate()));
    }

    public Exam getById(Long id) {
        return examRepository.findById(id).orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND", "Évaluation introuvable"));
    }

    public Page<Exam> list(Pageable pageable) {
        return examRepository.findAll(pageable);
    }

    @Transactional
    public Exam update(Long id, ExamRequest request) {
        validateReferences(request);
        Exam exam = getById(id);
        exam.setLabel(request.label());
        exam.setMaxScore(request.maxScore());
        exam.setCoefficient(request.coefficient());
        exam.setExamDate(request.examDate());
        return exam;
    }

    @Transactional
    public void delete(Long id) {
        examRepository.delete(getById(id));
    }

    private void validateReferences(ExamRequest request) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        if (subjectRepository.findById(request.subjectId()).isEmpty()) {
            throw ApiException.notFound("SUBJECT_NOT_FOUND", "Matière introuvable");
        }
    }
}
