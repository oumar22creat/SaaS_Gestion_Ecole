package com.schoolsaas.reportcard;

import com.schoolsaas.attendance.AttendanceRecord;
import com.schoolsaas.attendance.AttendanceRecordRepository;
import com.schoolsaas.attendance.AttendanceStatus;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.grade.GradeService;
import com.schoolsaas.grade.dto.SubjectAverageResponse;
import com.schoolsaas.reportcard.dto.GenerateReportCardsRequest;
import com.schoolsaas.reportcard.dto.ReportCardEntryUpdateRequest;
import com.schoolsaas.reportcard.dto.ReportCardUpdateRequest;
import com.schoolsaas.schoolclass.ClassSubjectAssignment;
import com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Génération automatique des bulletins (cahier-des-charges.md §12, ROADMAP.md 2.1) — réutilise
 * les moyennes déjà calculées par {@link GradeService} (ADR-013), pas de recalcul dupliqué.
 */
@Service
public class ReportCardService {

    private final ReportCardRepository reportCardRepository;
    private final ReportCardEntryRepository entryRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final ClassSubjectAssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final GradeService gradeService;

    public ReportCardService(
            ReportCardRepository reportCardRepository,
            ReportCardEntryRepository entryRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository,
            ClassSubjectAssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            GradeService gradeService) {
        this.reportCardRepository = reportCardRepository;
        this.entryRepository = entryRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.gradeService = gradeService;
    }

    @Transactional
    public List<ReportCard> generate(GenerateReportCardsRequest request) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }

        List<Long> subjectIds = assignmentRepository.findAllByClassId(request.schoolClassId()).stream()
                .map(ClassSubjectAssignment::getSubjectId)
                .distinct()
                .toList();
        Map<Long, Integer> coefficients = subjectRepository.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getCoefficient));

        List<Student> students = studentRepository.findAllBySchoolClassId(request.schoolClassId()).stream()
                .filter(Student::isActive)
                .toList();

        List<ReportCard> reportCards = students.stream()
                .map(student -> generateForStudent(student, subjectIds, coefficients, request))
                .toList();
        assignRanks(reportCards);
        return reportCards;
    }

    /**
     * Classe les bulletins par moyenne générale décroissante. Deux élèves à égalité partagent
     * le même rang et le suivant est décalé d'autant — « 1er, 1er, 3e » et non « 1er, 1er,
     * 2e » : c'est la convention des bulletins, et elle rend le rang cohérent avec l'effectif.
     *
     * <p>Un élève sans moyenne (aucune note sur la période) n'est pas classé : lui donner le
     * dernier rang le sanctionnerait pour une absence d'évaluation, pas pour ses résultats.
     * Il compte malgré tout dans l'effectif affiché, qui est celui de la classe.
     */
    private void assignRanks(List<ReportCard> reportCards) {
        List<ReportCard> ranked = reportCards.stream()
                .filter(card -> card.getGeneralAverage() != null)
                .sorted(Comparator.comparingDouble(ReportCard::getGeneralAverage).reversed())
                .toList();

        int classSize = reportCards.size();
        Double previousAverage = null;
        int previousRank = 0;
        for (int position = 0; position < ranked.size(); position++) {
            ReportCard card = ranked.get(position);
            int rank = card.getGeneralAverage().equals(previousAverage) ? previousRank : position + 1;
            card.setRankInClass(rank);
            card.setClassSize(classSize);
            previousAverage = card.getGeneralAverage();
            previousRank = rank;
        }
        reportCards.stream()
                .filter(card -> card.getGeneralAverage() == null)
                .forEach(card -> {
                    card.setRankInClass(null);
                    card.setClassSize(classSize);
                });
    }

    private ReportCard generateForStudent(
            Student student, List<Long> subjectIds, Map<Long, Integer> coefficients, GenerateReportCardsRequest request) {
        ReportCard reportCard = reportCardRepository.findByStudentIdAndPeriodLabel(student.getId(), request.periodLabel())
                .orElseGet(() -> reportCardRepository.save(new ReportCard(
                        student.getId(), request.schoolClassId(), request.periodLabel(), request.periodFrom(), request.periodTo())));

        // Régénération idempotente : on repart des lignes matière à chaque génération plutôt
        // que de les fusionner (une évaluation supprimée depuis ne doit pas laisser une ligne
        // obsolète).
        entryRepository.deleteAllByReportCardId(reportCard.getId());

        double weightedSum = 0;
        double coefficientSum = 0;
        for (Long subjectId : subjectIds) {
            int coefficient = coefficients.getOrDefault(subjectId, 1);
            SubjectAverageResponse subjectAverage = gradeService.studentSubjectAverage(student.getId(), subjectId);
            entryRepository.save(new ReportCardEntry(reportCard.getId(), subjectId, subjectAverage.average(), coefficient));
            if (subjectAverage.average() != null) {
                weightedSum += subjectAverage.average() * coefficient;
                coefficientSum += coefficient;
            }
        }
        reportCard.setGeneralAverage(coefficientSum == 0 ? null : NumberUtils.round2(weightedSum / coefficientSum));

        List<AttendanceRecord> records = attendanceRecordRepository.findAllByStudentIdAndDateBetween(
                student.getId(), request.periodFrom(), request.periodTo());
        reportCard.setAbsenceCount((int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count());
        reportCard.setLateCount((int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count());

        return reportCard;
    }

    public ReportCard getById(Long id) {
        return reportCardRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("REPORT_CARD_NOT_FOUND", "Bulletin introuvable"));
    }

    public List<ReportCardEntry> listEntries(Long reportCardId) {
        return entryRepository.findAllByReportCardId(reportCardId);
    }

    public List<ReportCard> listForStudent(Long studentId) {
        return reportCardRepository.findAllByStudentId(studentId);
    }

    public List<ReportCard> listForClassAndPeriod(Long schoolClassId, String periodLabel) {
        return reportCardRepository.findAllBySchoolClassIdAndPeriodLabel(schoolClassId, periodLabel);
    }

    @Transactional
    public ReportCard update(Long id, ReportCardUpdateRequest request) {
        ReportCard reportCard = getById(id);
        reportCard.setGeneralComment(request.generalComment());
        reportCard.setCouncilDecision(request.councilDecision());
        return reportCard;
    }

    @Transactional
    public ReportCardEntry updateEntry(Long reportCardId, Long subjectId, ReportCardEntryUpdateRequest request) {
        ReportCardEntry entry = entryRepository.findByReportCardIdAndSubjectId(reportCardId, subjectId)
                .orElseThrow(() -> ApiException.notFound("REPORT_CARD_ENTRY_NOT_FOUND", "Ligne de bulletin introuvable"));
        entry.setTeacherComment(request.teacherComment());
        return entry;
    }
}
