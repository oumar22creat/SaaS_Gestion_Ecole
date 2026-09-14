package com.schoolsaas.statistics;

import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.statistics.dto.GradeEvolutionPointResponse;
import com.schoolsaas.statistics.dto.ResultsEvolutionResponse;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/**
 * Statistiques avancées — cahier-des-charges.md §18, ROADMAP.md 3.2. Étend le dashboard
 * établissement de base (1.9/ADR-014) avec une vue dans le temps, absente de ce dernier.
 */
@Service
public class AdvancedStatisticsService {

    private final ExamRepository examRepository;
    private final GradeRepository gradeRepository;

    public AdvancedStatisticsService(ExamRepository examRepository, GradeRepository gradeRepository) {
        this.examRepository = examRepository;
        this.gradeRepository = gradeRepository;
    }

    /**
     * Moyenne normalisée sur 20, regroupée par mois calendaire de la date d'évaluation (aucune
     * notion de trimestre/période pédagogique paramétrable n'existe, voir ADR-010) — pour une
     * classe entière, ou pour une seule matière si {@code subjectId} est fourni.
     */
    public ResultsEvolutionResponse resultsEvolution(Long schoolClassId, Long subjectId) {
        List<Exam> exams = subjectId != null
                ? examRepository.findAllBySchoolClassIdAndSubjectId(schoolClassId, subjectId)
                : examRepository.findAllBySchoolClassId(schoolClassId);

        Map<YearMonth, List<Double>> scoresByPeriod = new TreeMap<>();
        for (Exam exam : exams) {
            List<Double> normalized = gradeRepository.findAllByExamId(exam.getId()).stream()
                    .filter(grade -> !grade.isAbsent() && grade.getScore() != null)
                    .map(grade -> grade.getScore() / exam.getMaxScore() * 20.0)
                    .toList();
            scoresByPeriod.computeIfAbsent(YearMonth.from(exam.getExamDate()), k -> new ArrayList<>()).addAll(normalized);
        }

        List<GradeEvolutionPointResponse> points = scoresByPeriod.entrySet().stream()
                .map(entry -> new GradeEvolutionPointResponse(
                        entry.getKey().toString(),
                        entry.getValue().isEmpty()
                                ? null
                                : NumberUtils.round2(entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)),
                        entry.getValue().size()))
                .toList();
        return new ResultsEvolutionResponse(schoolClassId, subjectId, points);
    }

    /** Export CSV de {@link #resultsEvolution} — seul format d'export construit pour cette passe (voir ADR-023). */
    public String resultsEvolutionCsv(Long schoolClassId, Long subjectId) {
        StringBuilder csv = new StringBuilder("period,average,gradeCount\n");
        for (GradeEvolutionPointResponse point : resultsEvolution(schoolClassId, subjectId).points()) {
            csv.append(point.period()).append(',')
                    .append(point.average() == null ? "" : point.average()).append(',')
                    .append(point.gradeCount()).append('\n');
        }
        return csv.toString();
    }
}
