package com.schoolsaas.portal;

import com.schoolsaas.attendance.AttendanceService;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.Grade;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.paperwork.PaperworkService;
import com.schoolsaas.portal.dto.PortalResponse;
import com.schoolsaas.schoolfees.FeeInvoiceStatus;
import com.schoolsaas.schoolfees.FeePayment;
import com.schoolsaas.schoolfees.FeePaymentRepository;
import com.schoolsaas.schoolfees.FeeSchedule;
import com.schoolsaas.schoolfees.FeeScheduleRepository;
import com.schoolsaas.schoolfees.StudentFeeInvoice;
import com.schoolsaas.schoolfees.StudentFeeInvoiceRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.timetable.Room;
import com.schoolsaas.timetable.RoomRepository;
import com.schoolsaas.timetable.TimetableEntry;
import com.schoolsaas.timetable.TimetableEntryRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portail consulté par les familles depuis l'application mobile (cahier §13/§14).
 *
 * <p>Chaque méthode passe par {@link PortalService#requireAccessibleStudent} : le contrôle ne
 * repose pas sur {@code @PreAuthorize}, qui ne sait vérifier qu'un rôle, alors que la vraie
 * question ici est « cet élève appartient-il à cette famille ? ».
 */
@RestController
@RequestMapping("/api/v1/portal")
@PreAuthorize("hasAnyRole('PARENT', 'STUDENT')")
public class PortalController {

    /** XOF : franc CFA, devise sans sous-unité — les montants « cents » sont des francs entiers. */
    private static final String DEFAULT_CURRENCY = "XOF";

    private final PortalService portalService;
    private final AttendanceService attendanceService;
    private final GradeRepository gradeRepository;
    private final ExamRepository examRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final RoomRepository roomRepository;
    private final StudentFeeInvoiceRepository invoiceRepository;
    private final FeeScheduleRepository feeScheduleRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final PaperworkService paperworkService;

    public PortalController(
            PortalService portalService,
            AttendanceService attendanceService,
            GradeRepository gradeRepository,
            ExamRepository examRepository,
            TimetableEntryRepository timetableEntryRepository,
            SubjectRepository subjectRepository,
            TeacherRepository teacherRepository,
            RoomRepository roomRepository,
            StudentFeeInvoiceRepository invoiceRepository,
            FeeScheduleRepository feeScheduleRepository,
            FeePaymentRepository feePaymentRepository,
            PaperworkService paperworkService) {
        this.portalService = portalService;
        this.attendanceService = attendanceService;
        this.gradeRepository = gradeRepository;
        this.examRepository = examRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.roomRepository = roomRepository;
        this.invoiceRepository = invoiceRepository;
        this.feeScheduleRepository = feeScheduleRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.paperworkService = paperworkService;
    }

    /** Enfants du parent connecté, ou l'élève lui-même s'il consulte son propre portail. */
    @GetMapping("/children")
    public ApiResponse<List<PortalResponse.Child>> children() {
        return ApiResponse.of(portalService.accessibleStudents().stream()
                .map(PortalController::toChild)
                .toList());
    }

    @GetMapping("/students/{studentId}/grades")
    public ApiResponse<List<PortalResponse.GradeLine>> grades(@PathVariable Long studentId) {
        portalService.requireAccessibleStudent(studentId);

        List<Grade> grades = gradeRepository.findAllByStudentId(studentId);
        Map<Long, Exam> examsById = examRepository
                .findAllById(grades.stream().map(Grade::getExamId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));

        return ApiResponse.of(grades.stream()
                .filter(grade -> examsById.containsKey(grade.getExamId()))
                .map(grade -> {
                    Exam exam = examsById.get(grade.getExamId());
                    return new PortalResponse.GradeLine(
                            exam.getId(),
                            exam.getLabel(),
                            exam.getExamDate(),
                            grade.getScore(),
                            exam.getMaxScore(),
                            exam.getCoefficient(),
                            grade.isAbsent());
                })
                .sorted((a, b) -> b.examDate().compareTo(a.examDate()))
                .toList());
    }

    /**
     * Frais de scolarité de l'élève (cahier §13). Une famille pouvait consulter les notes et
     * les absences de son enfant mais pas ce qu'elle devait : la question la plus concrète
     * qu'elle se pose restait sans réponse dans l'application.
     *
     * <p>Aucune action n'est exposée ici : le portail informe, l'encaissement reste au
     * personnel. Les montants sont recalculés depuis les règlements enregistrés, jamais repris
     * d'un champ transmis par le client.
     */
    @GetMapping("/students/{studentId}/fees")
    public ApiResponse<PortalResponse.FeeSummary> fees(@PathVariable Long studentId) {
        portalService.requireAccessibleStudent(studentId);

        List<StudentFeeInvoice> invoices = invoiceRepository.findAllByStudentIdOrderByIssuedAtDesc(studentId).stream()
                // Une facture annulée n'est plus due : l'afficher inquiéterait une famille pour rien.
                .filter(invoice -> invoice.getStatus() != FeeInvoiceStatus.CANCELLED)
                .toList();
        if (invoices.isEmpty()) {
            return ApiResponse.of(new PortalResponse.FeeSummary(0, 0, 0, 0, DEFAULT_CURRENCY, List.of()));
        }

        Map<Long, FeeSchedule> schedulesById = feeScheduleRepository
                .findAllById(invoices.stream().map(StudentFeeInvoice::getFeeScheduleId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(FeeSchedule::getId, Function.identity()));
        Map<Long, Long> paidByInvoice = feePaymentRepository
                .findAllByInvoiceIdIn(invoices.stream().map(StudentFeeInvoice::getId).toList()).stream()
                .collect(Collectors.groupingBy(
                        FeePayment::getInvoiceId, Collectors.summingLong(FeePayment::getAmountCents)));
        LocalDate today = LocalDate.now();

        List<PortalResponse.FeeLine> lines = invoices.stream()
                .map(invoice -> {
                    FeeSchedule schedule = schedulesById.get(invoice.getFeeScheduleId());
                    long paid = paidByInvoice.getOrDefault(invoice.getId(), 0L);
                    long remaining = invoice.getAmountDueCents() - paid;
                    LocalDate dueDate = schedule == null ? null : schedule.getDueDate();
                    boolean overdue = remaining > 0 && dueDate != null && dueDate.isBefore(today);
                    return new PortalResponse.FeeLine(
                            schedule == null ? "Frais de scolarité" : schedule.getLabel(),
                            dueDate,
                            invoice.getAmountDueCents(),
                            paid,
                            remaining,
                            invoice.getStatus().name(),
                            overdue);
                })
                .toList();

        return ApiResponse.of(new PortalResponse.FeeSummary(
                lines.stream().mapToLong(PortalResponse.FeeLine::amountDueCents).sum(),
                lines.stream().mapToLong(PortalResponse.FeeLine::amountPaidCents).sum(),
                lines.stream().mapToLong(PortalResponse.FeeLine::amountRemainingCents).sum(),
                lines.stream().filter(PortalResponse.FeeLine::overdue)
                        .mapToLong(PortalResponse.FeeLine::amountRemainingCents).sum(),
                schedulesById.values().stream().findFirst().map(FeeSchedule::getCurrency).orElse(DEFAULT_CURRENCY),
                lines));
    }

    @GetMapping("/students/{studentId}/attendance")
    public ApiResponse<List<PortalResponse.AttendanceLine>> attendance(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        portalService.requireAccessibleStudent(studentId);

        return ApiResponse.of(attendanceService.listHistoryForStudent(studentId, from, to).stream()
                .map(record -> new PortalResponse.AttendanceLine(
                        record.getDate(),
                        record.getStatus().name(),
                        record.getReason(),
                        record.isJustified()))
                .toList());
    }

    /**
     * Emploi du temps de la classe de l'élève (mockup docs/MOCKUPS.md « Élève — emploi du
     * temps »). Les noms de matière, d'enseignant et de salle sont résolus ici : une famille
     * ne peut rien faire d'un identifiant technique.
     */
    /**
     * Les règlements encaissés pour cet élève, du plus récent au plus ancien.
     *
     * <p>La scolarité se paie souvent en espèces au guichet, parfois par un proche. Sans
     * cette liste, le parent qui ne s'est pas déplacé n'a aucune trace de ce qui a été versé
     * en son nom, et doit rappeler le secrétariat pour le savoir.
     */
    @GetMapping("/students/{studentId}/receipts")
    public ApiResponse<List<PortalResponse.ReceiptLine>> receipts(@PathVariable Long studentId) {
        portalService.requireAccessibleStudent(studentId);

        List<StudentFeeInvoice> factures = invoiceRepository.findAllByStudentIdOrderByIssuedAtDesc(studentId);
        Map<Long, String> libelles = libellesParFacture(factures);
        return ApiResponse.of(feePaymentRepository
                .findAllByInvoiceIdIn(factures.stream().map(StudentFeeInvoice::getId).toList()).stream()
                .sorted(Comparator.comparing(FeePayment::getPaidAt).reversed())
                .map(reglement -> new PortalResponse.ReceiptLine(
                        reglement.getId(),
                        reglement.getPaidAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                        libelles.getOrDefault(reglement.getInvoiceId(), "Frais de scolarité"),
                        reglement.getAmountCents(),
                        reglement.getMethod().name(),
                        reglement.getReference()))
                .toList());
    }

    /**
     * Le reçu lui-même, en PDF — le même document que celui délivré au guichet.
     *
     * <p>L'accès est vérifié en remontant du règlement à l'élève, et non en faisant confiance
     * à l'identifiant reçu : sans ce contrôle, un parent connecté pourrait télécharger le
     * reçu de n'importe quel élève de l'établissement en changeant un numéro dans l'URL.
     */
    @GetMapping("/payments/{paymentId}/receipt.pdf")
    public ResponseEntity<byte[]> receiptPdf(@PathVariable Long paymentId) {
        FeePayment reglement = feePaymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Règlement introuvable"));
        StudentFeeInvoice facture = invoiceRepository.findById(reglement.getInvoiceId())
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Règlement introuvable"));
        portalService.requireAccessibleStudent(facture.getStudentId());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("recu-" + paymentId + ".pdf").build().toString())
                .body(paperworkService.paymentReceipt(paymentId));
    }

    private Map<Long, String> libellesParFacture(List<StudentFeeInvoice> factures) {
        Map<Long, String> parEcheance = feeScheduleRepository
                .findAllById(factures.stream().map(StudentFeeInvoice::getFeeScheduleId).distinct().toList()).stream()
                .collect(Collectors.toMap(FeeSchedule::getId, FeeSchedule::getLabel, (a, b) -> a));
        return factures.stream().collect(Collectors.toMap(
                StudentFeeInvoice::getId,
                facture -> parEcheance.getOrDefault(facture.getFeeScheduleId(), "Frais de scolarité"),
                (a, b) -> a));
    }

    @GetMapping("/students/{studentId}/timetable")
    public ApiResponse<List<PortalResponse.TimetableSlot>> timetable(@PathVariable Long studentId) {
        Student student = portalService.requireAccessibleStudent(studentId);
        if (student.getSchoolClassId() == null) {
            return ApiResponse.of(List.of());
        }

        List<TimetableEntry> entries =
                timetableEntryRepository.findAllBySchoolClassId(student.getSchoolClassId());
        Map<Long, String> subjects = subjectRepository
                .findAllById(entries.stream().map(TimetableEntry::getSubjectId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
        Map<Long, String> teachers = teacherRepository
                .findAllById(entries.stream().map(TimetableEntry::getTeacherId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(
                        Teacher::getId, teacher -> teacher.getFirstName() + " " + teacher.getLastName()));
        Map<Long, String> rooms = roomRepository
                .findAllById(entries.stream()
                        .map(TimetableEntry::getRoomId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(Room::getId, Room::getName));

        return ApiResponse.of(entries.stream()
                .sorted(java.util.Comparator.comparing(TimetableEntry::getDayOfWeek)
                        .thenComparing(TimetableEntry::getStartTime))
                .map(entry -> new PortalResponse.TimetableSlot(
                        entry.getDayOfWeek().name(),
                        entry.getStartTime().toString(),
                        entry.getEndTime().toString(),
                        subjects.getOrDefault(entry.getSubjectId(), "Matière"),
                        teachers.getOrDefault(entry.getTeacherId(), ""),
                        entry.getRoomId() == null ? "" : rooms.getOrDefault(entry.getRoomId(), "")))
                .toList());
    }

    private static PortalResponse.Child toChild(Student student) {
        return new PortalResponse.Child(
                student.getId(),
                student.getFirstName(),
                student.getLastName(),
                student.getStudentNumber(),
                student.getSchoolClassId());
    }
}
