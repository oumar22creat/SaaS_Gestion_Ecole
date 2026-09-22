package com.schoolsaas.statistics;

import com.schoolsaas.attendance.AttendanceRecord;
import com.schoolsaas.attendance.AttendanceRecordRepository;
import com.schoolsaas.attendance.AttendanceStatus;
import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.Grade;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolfees.SchoolFeesService;
import com.schoolsaas.schoolfees.dto.FeeSummaryResponse;
import com.schoolsaas.statistics.dto.DashboardSummaryResponse;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.teacher.TeacherRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Tableau de bord établissement — cahier-des-charges.md §18, ROADMAP.md 1.9. Statistiques
 * "de base" seulement (effectifs, taux de présence, moyennes, recouvrement des frais) : pas
 * de ventilation par
 * classe/matière (déjà couverte par les endpoints du module grade, voir ADR-013), pas
 * d'évolution temporelle ni de rapports exportables (hors périmètre 1.9, voir ADR-014).
 */
@Service
public class DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final GradeRepository gradeRepository;
    private final ExamRepository examRepository;
    private final SchoolFeesService schoolFeesService;

    public DashboardService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            SchoolClassRepository schoolClassRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            GradeRepository gradeRepository,
            ExamRepository examRepository,
            SchoolFeesService schoolFeesService) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.gradeRepository = gradeRepository;
        this.examRepository = examRepository;
        this.schoolFeesService = schoolFeesService;
    }

    public DashboardSummaryResponse summary(LocalDate from, LocalDate to) {
        long studentCount = studentRepository.countByActiveTrue();
        long teacherCount = teacherRepository.countByActiveTrue();
        long classCount = schoolClassRepository.count();

        return new DashboardSummaryResponse(
                studentCount, teacherCount, classCount, from, to, attendanceRate(from, to), averageGrade(), finance());
    }

    /**
     * Recouvrement des frais de scolarité, sur tout l'établissement. Renvoie null tant
     * qu'aucune facture n'a été émise : afficher « 0 % recouvré » à un établissement qui n'a
     * simplement rien facturé serait une fausse alerte.
     */
    private FeeSummaryResponse finance() {
        FeeSummaryResponse summary = schoolFeesService.summary(null);
        return summary.invoiceCount() == 0 ? null : summary;
    }

    private Double attendanceRate(LocalDate from, LocalDate to) {
        List<AttendanceRecord> records = attendanceRecordRepository.findAllByDateBetween(from, to);
        if (records.isEmpty()) {
            return null;
        }
        long presentCount = records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        return NumberUtils.round2(100.0 * presentCount / records.size());
    }

    private Double averageGrade() {
        List<Grade> grades = gradeRepository.findAllByAbsentFalseAndScoreIsNotNull();
        if (grades.isEmpty()) {
            return null;
        }
        Map<Long, Exam> examsById = examRepository.findAllById(grades.stream().map(Grade::getExamId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Exam::getId, e -> e));

        double sum = 0;
        int count = 0;
        for (Grade grade : grades) {
            Exam exam = examsById.get(grade.getExamId());
            if (exam == null) {
                continue;
            }
            sum += grade.getScore() / exam.getMaxScore() * 20.0;
            count++;
        }
        return count == 0 ? null : NumberUtils.round2(sum / count);
    }
}
