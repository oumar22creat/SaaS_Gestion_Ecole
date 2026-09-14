package com.schoolsaas.grade;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.grade.dto.ClassSubjectAverageResponse;
import com.schoolsaas.grade.dto.ExamStatisticsResponse;
import com.schoolsaas.grade.dto.GradeEntryRequest;
import com.schoolsaas.grade.dto.SubjectAverageResponse;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saisie des notes + calcul automatique des moyennes — cahier-des-charges.md §11, ROADMAP.md 1.8. */
@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;

    public GradeService(GradeRepository gradeRepository, ExamRepository examRepository, StudentRepository studentRepository) {
        this.gradeRepository = gradeRepository;
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public List<Grade> submit(Long examId, List<GradeEntryRequest> entries) {
        Exam exam = getExam(examId);
        return entries.stream().map(entry -> upsert(exam, entry)).toList();
    }

    private Grade upsert(Exam exam, GradeEntryRequest entry) {
        if (studentRepository.findById(entry.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable : " + entry.studentId());
        }
        validateScore(exam, entry.score(), entry.absent());
        return gradeRepository.findByExamIdAndStudentId(exam.getId(), entry.studentId())
                .map(existing -> {
                    existing.update(entry.score(), entry.absent(), entry.comment());
                    return existing;
                })
                .orElseGet(() -> gradeRepository.save(
                        new Grade(exam.getId(), entry.studentId(), entry.score(), entry.absent(), entry.comment())));
    }

    @Transactional
    public Grade updateOne(Long examId, Long studentId, GradeEntryRequest entry) {
        Exam exam = getExam(examId);
        validateScore(exam, entry.score(), entry.absent());
        Grade grade = gradeRepository.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND", "Note introuvable"));
        grade.update(entry.score(), entry.absent(), entry.comment());
        return grade;
    }

    public List<Grade> listForExam(Long examId) {
        return gradeRepository.findAllByExamId(examId);
    }

    /** Moyenne de classe, minimum, maximum pour une évaluation (cahier §11). */
    public ExamStatisticsResponse examStatistics(Long examId) {
        Exam exam = getExam(examId);
        List<Double> normalizedScores = gradeRepository.findAllByExamId(examId).stream()
                .filter(g -> !g.isAbsent() && g.getScore() != null)
                .map(g -> normalize(g.getScore(), exam.getMaxScore()))
                .toList();
        if (normalizedScores.isEmpty()) {
            return new ExamStatisticsResponse(examId, 0, null, null, null);
        }
        double average = normalizedScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = normalizedScores.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = normalizedScores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return new ExamStatisticsResponse(examId, normalizedScores.size(), NumberUtils.round2(average), NumberUtils.round2(min), NumberUtils.round2(max));
    }

    /** Moyenne pondérée par coefficient d'évaluation, normalisée sur 20, pour un élève dans une matière. */
    public SubjectAverageResponse studentSubjectAverage(Long studentId, Long subjectId) {
        List<Exam> exams = examRepository.findAllBySubjectId(subjectId);
        if (exams.isEmpty()) {
            return new SubjectAverageResponse(subjectId, null, 0);
        }
        List<Long> examIds = exams.stream().map(Exam::getId).toList();
        Map<Long, Exam> examsById = exams.stream().collect(Collectors.toMap(Exam::getId, e -> e));

        List<Grade> grades = gradeRepository.findAllByExamIdInAndStudentId(examIds, studentId).stream()
                .filter(g -> !g.isAbsent() && g.getScore() != null)
                .toList();
        if (grades.isEmpty()) {
            return new SubjectAverageResponse(subjectId, null, 0);
        }

        double weightedSum = 0;
        double coefficientSum = 0;
        for (Grade grade : grades) {
            Exam exam = examsById.get(grade.getExamId());
            weightedSum += normalize(grade.getScore(), exam.getMaxScore()) * exam.getCoefficient();
            coefficientSum += exam.getCoefficient();
        }
        return new SubjectAverageResponse(subjectId, NumberUtils.round2(weightedSum / coefficientSum), grades.size());
    }

    /** Moyenne de classe pour une matière : moyenne des moyennes élèves ayant au moins une note. */
    public ClassSubjectAverageResponse classSubjectAverage(Long schoolClassId, Long subjectId) {
        List<Student> students = studentRepository.findAllBySchoolClassId(schoolClassId);
        List<Double> studentAverages = students.stream()
                .map(student -> studentSubjectAverage(student.getId(), subjectId).average())
                .filter(java.util.Objects::nonNull)
                .toList();
        if (studentAverages.isEmpty()) {
            return new ClassSubjectAverageResponse(schoolClassId, subjectId, null, 0);
        }
        double average = studentAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new ClassSubjectAverageResponse(schoolClassId, subjectId, NumberUtils.round2(average), studentAverages.size());
    }

    private void validateScore(Exam exam, Double score, boolean absent) {
        if (!absent && score == null) {
            throw ApiException.badRequest("SCORE_REQUIRED", "Une note est requise pour un élève non absent", List.of());
        }
        if (score != null && (score < 0 || score > exam.getMaxScore())) {
            throw ApiException.badRequest("SCORE_OUT_OF_RANGE", "La note doit être comprise entre 0 et " + exam.getMaxScore(), List.of());
        }
    }

    private double normalize(double score, double maxScore) {
        return score / maxScore * 20.0;
    }

    private Exam getExam(Long examId) {
        return examRepository.findById(examId).orElseThrow(() -> ApiException.notFound("EXAM_NOT_FOUND", "Évaluation introuvable"));
    }
}
