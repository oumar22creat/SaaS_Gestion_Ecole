package com.schoolsaas.schoolfees;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolfees.dto.FeeOutstandingEntry;
import com.schoolsaas.schoolfees.dto.FeePaymentCreateRequest;
import com.schoolsaas.schoolfees.dto.FeePaymentJournalEntry;
import com.schoolsaas.schoolfees.dto.FeeReportingResponse;
import com.schoolsaas.schoolfees.dto.FeeScheduleCreateRequest;
import com.schoolsaas.schoolfees.dto.FeeSummaryResponse;
import com.schoolsaas.schoolfees.dto.StudentFeeInvoiceResponse;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comptabilité et frais scolaires — cahier-des-charges.md §19.4, ROADMAP.md 3.3. */
@Service
public class SchoolFeesService {

    /** XOF : franc CFA, devise sans sous-unité — les montants « cents » sont donc des francs entiers. */
    private static final String DEFAULT_CURRENCY = "XOF";

    private final FeeScheduleRepository feeScheduleRepository;
    private final StudentFeeInvoiceRepository invoiceRepository;
    private final FeePaymentRepository paymentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    public SchoolFeesService(
            FeeScheduleRepository feeScheduleRepository,
            StudentFeeInvoiceRepository invoiceRepository,
            FeePaymentRepository paymentRepository,
            SchoolClassRepository schoolClassRepository,
            StudentRepository studentRepository) {
        this.feeScheduleRepository = feeScheduleRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public FeeSchedule createSchedule(FeeScheduleCreateRequest request) {
        if (schoolClassRepository.findById(request.schoolClassId()).isEmpty()) {
            throw ApiException.notFound("SCHOOL_CLASS_NOT_FOUND", "Classe introuvable");
        }
        return feeScheduleRepository.save(
                new FeeSchedule(request.schoolClassId(), request.label(), request.amountCents(), DEFAULT_CURRENCY, request.dueDate()));
    }

    public List<FeeSchedule> listSchedulesForClass(Long schoolClassId) {
        return feeScheduleRepository.findAllBySchoolClassId(schoolClassId);
    }

    public FeeSchedule getSchedule(Long id) {
        return feeScheduleRepository.findById(id).orElseThrow(() -> ApiException.notFound("FEE_SCHEDULE_NOT_FOUND", "Grille tarifaire introuvable"));
    }

    /** Génère une facture par élève actif de la classe, idempotent (une seule facture par élève et par grille). */
    @Transactional
    public List<StudentFeeInvoice> generateInvoices(Long scheduleId) {
        FeeSchedule schedule = getSchedule(scheduleId);
        List<Student> activeStudents = studentRepository.findAllBySchoolClassId(schedule.getSchoolClassId()).stream()
                .filter(Student::isActive)
                .toList();
        return activeStudents.stream()
                .map(student -> invoiceRepository.findByFeeScheduleIdAndStudentId(scheduleId, student.getId())
                        .orElseGet(() -> invoiceRepository.save(
                                new StudentFeeInvoice(scheduleId, student.getId(), schedule.getAmountCents()))))
                .toList();
    }

    public StudentFeeInvoice getInvoice(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> ApiException.notFound("INVOICE_NOT_FOUND", "Facture introuvable"));
    }

    public List<StudentFeeInvoice> invoicesForStudent(Long studentId) {
        return invoiceRepository.findAllByStudentIdOrderByIssuedAtDesc(studentId);
    }

    public long paidAmountForInvoice(Long invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId).stream().mapToLong(FeePayment::getAmountCents).sum();
    }

    public List<FeePayment> paymentsForInvoice(Long invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId);
    }

    @Transactional
    public FeePayment recordPayment(Long invoiceId, FeePaymentCreateRequest request, Long recordedByUserId) {
        StudentFeeInvoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == FeeInvoiceStatus.CANCELLED) {
            throw ApiException.badRequest("INVOICE_CANCELLED", "Cette facture est annulée", List.of());
        }
        long alreadyPaid = paidAmountForInvoice(invoiceId);
        long remaining = invoice.getAmountDueCents() - alreadyPaid;
        if (request.amountCents() > remaining) {
            // Sans ce garde-fou, une erreur de frappe gonflait les encaissements et le taux de
            // recouvrement pouvait dépasser 100 % sans qu'aucun écran ne le signale. Un
            // trop-perçu doit être traité pour ce qu'il est, pas absorbé silencieusement.
            throw ApiException.unprocessable(
                    "PAYMENT_EXCEEDS_BALANCE",
                    "Le montant dépasse le solde restant de cette facture (" + remaining + ")");
        }
        FeePayment payment = paymentRepository.save(
                new FeePayment(invoiceId, request.amountCents(), request.method(), request.reference(), recordedByUserId));

        long totalPaid = alreadyPaid + request.amountCents();
        invoice.setStatus(
                totalPaid >= invoice.getAmountDueCents() ? FeeInvoiceStatus.PAID
                        : totalPaid > 0 ? FeeInvoiceStatus.PARTIALLY_PAID : FeeInvoiceStatus.PENDING);
        return payment;
    }

    public FeeReportingResponse reporting(Long schoolClassId, LocalDate from, LocalDate to) {
        List<FeeSchedule> schedules = feeScheduleRepository.findAllBySchoolClassIdAndDueDateBetween(schoolClassId, from, to);
        List<StudentFeeInvoice> invoices = schedules.stream()
                .flatMap(schedule -> invoiceRepository.findAllByFeeScheduleId(schedule.getId()).stream())
                .toList();

        long totalDueCents = invoices.stream().mapToLong(StudentFeeInvoice::getAmountDueCents).sum();
        long totalPaidCents = invoices.stream().mapToLong(invoice -> paidAmountForInvoice(invoice.getId())).sum();

        List<StudentFeeInvoiceResponse> unpaid = invoices.stream()
                .filter(invoice -> invoice.getStatus() != FeeInvoiceStatus.PAID && invoice.getStatus() != FeeInvoiceStatus.CANCELLED)
                .map(invoice -> StudentFeeInvoiceResponse.from(invoice, paidAmountForInvoice(invoice.getId())))
                .toList();

        return new FeeReportingResponse(schoolClassId, from, to, totalDueCents, totalPaidCents, totalDueCents - totalPaidCents, unpaid);
    }

    // ------------------------------------------------------------------------------------
    // Vue d'ensemble de l'établissement
    //
    // Les méthodes ci-dessous travaillent sur tout l'établissement, une classe donnée n'étant
    // qu'un filtre optionnel : un comptable suit le recouvrement de l'école, pas d'une classe
    // à la fois. Elles chargent les paiements en une requête groupée plutôt qu'une par
    // facture — sur un établissement de plusieurs centaines d'élèves, la seconde approche
    // rendait l'écran inutilisable.
    // ------------------------------------------------------------------------------------

    /** Factures non soldées, les plus en retard d'abord. {@code onlyOverdue} restreint aux échéances dépassées. */
    public List<FeeOutstandingEntry> outstanding(Long schoolClassId, boolean onlyOverdue) {
        List<FeeSchedule> schedules = schedulesFor(schoolClassId);
        if (schedules.isEmpty()) {
            return List.of();
        }
        Map<Long, FeeSchedule> schedulesById = schedules.stream()
                .collect(Collectors.toMap(FeeSchedule::getId, Function.identity()));

        List<StudentFeeInvoice> invoices = invoiceRepository.findAllByFeeScheduleIdIn(List.copyOf(schedulesById.keySet())).stream()
                .filter(invoice -> invoice.getStatus() != FeeInvoiceStatus.PAID)
                .filter(invoice -> invoice.getStatus() != FeeInvoiceStatus.CANCELLED)
                .toList();
        if (invoices.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> paidByInvoice = paidAmountsByInvoice(invoices);
        Map<Long, Student> studentsById = studentsById(invoices);
        Map<Long, String> classNames = classNames();
        LocalDate today = LocalDate.now();

        return invoices.stream()
                .map(invoice -> toOutstandingEntry(
                        invoice, schedulesById.get(invoice.getFeeScheduleId()),
                        studentsById.get(invoice.getStudentId()),
                        paidByInvoice.getOrDefault(invoice.getId(), 0L), classNames, today))
                .filter(entry -> entry.amountRemainingCents() > 0)
                .filter(entry -> !onlyOverdue || entry.overdue())
                // Le plus en retard d'abord : c'est l'ordre dans lequel on relance.
                .sorted(Comparator.comparing(FeeOutstandingEntry::daysLate).reversed()
                        .thenComparing(FeeOutstandingEntry::studentName))
                .toList();
    }

    /** Indicateurs de recouvrement de l'établissement, ou d'une classe si {@code schoolClassId} est fourni. */
    public FeeSummaryResponse summary(Long schoolClassId) {
        List<FeeSchedule> schedules = schedulesFor(schoolClassId);
        if (schedules.isEmpty()) {
            return new FeeSummaryResponse(0, 0, 0, 0, null, 0, 0, 0, 0, DEFAULT_CURRENCY, List.of());
        }
        Map<Long, FeeSchedule> schedulesById = schedules.stream()
                .collect(Collectors.toMap(FeeSchedule::getId, Function.identity()));

        // Une facture annulée n'est ni due ni recouvrable : la compter écraserait le taux de
        // recouvrement vers le bas sans qu'aucun impayé réel n'existe.
        List<StudentFeeInvoice> invoices = invoiceRepository.findAllByFeeScheduleIdIn(List.copyOf(schedulesById.keySet())).stream()
                .filter(invoice -> invoice.getStatus() != FeeInvoiceStatus.CANCELLED)
                .toList();
        if (invoices.isEmpty()) {
            return new FeeSummaryResponse(0, 0, 0, 0, null, 0, 0, 0, 0, DEFAULT_CURRENCY, List.of());
        }

        Map<Long, Long> paidByInvoice = paidAmountsByInvoice(invoices);
        LocalDate today = LocalDate.now();

        long invoicedCents = invoices.stream().mapToLong(StudentFeeInvoice::getAmountDueCents).sum();
        long collectedCents = paidByInvoice.values().stream().mapToLong(Long::longValue).sum();
        long settledCount = invoices.stream().filter(invoice -> invoice.getStatus() == FeeInvoiceStatus.PAID).count();

        List<StudentFeeInvoice> overdue = invoices.stream()
                .filter(invoice -> invoice.getStatus() != FeeInvoiceStatus.PAID)
                .filter(invoice -> remaining(invoice, paidByInvoice) > 0)
                .filter(invoice -> isOverdue(schedulesById.get(invoice.getFeeScheduleId()), today))
                .toList();

        return new FeeSummaryResponse(
                invoicedCents,
                collectedCents,
                invoicedCents - collectedCents,
                overdue.stream().mapToLong(invoice -> remaining(invoice, paidByInvoice)).sum(),
                invoicedCents == 0 ? null : NumberUtils.round2(100.0 * collectedCents / invoicedCents),
                invoices.size(),
                settledCount,
                overdue.size(),
                overdue.stream().map(StudentFeeInvoice::getStudentId).distinct().count(),
                schedules.getFirst().getCurrency(),
                collectionByMethod(invoices));
    }

    /** Journal des encaissements d'une période, le plus récent d'abord. */
    public List<FeePaymentJournalEntry> paymentJournal(LocalDate from, LocalDate to) {
        List<FeePayment> payments = paymentRepository.findAllByPaidAtBetweenOrderByPaidAtDesc(
                from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        if (payments.isEmpty()) {
            return List.of();
        }

        Map<Long, StudentFeeInvoice> invoicesById = invoiceRepository
                .findAllById(payments.stream().map(FeePayment::getInvoiceId).distinct().toList()).stream()
                .collect(Collectors.toMap(StudentFeeInvoice::getId, Function.identity()));
        Map<Long, FeeSchedule> schedulesById = feeScheduleRepository
                .findAllById(invoicesById.values().stream().map(StudentFeeInvoice::getFeeScheduleId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(FeeSchedule::getId, Function.identity()));
        Map<Long, Student> studentsById = studentsById(invoicesById.values().stream().toList());
        Map<Long, String> classNames = classNames();

        return payments.stream()
                .map(payment -> {
                    StudentFeeInvoice invoice = invoicesById.get(payment.getInvoiceId());
                    FeeSchedule schedule = invoice == null ? null : schedulesById.get(invoice.getFeeScheduleId());
                    Student student = invoice == null ? null : studentsById.get(invoice.getStudentId());
                    return new FeePaymentJournalEntry(
                            payment.getId(),
                            payment.getInvoiceId(),
                            invoice == null ? null : invoice.getStudentId(),
                            studentName(student),
                            schedule == null ? "—" : classNames.getOrDefault(schedule.getSchoolClassId(), "—"),
                            schedule == null ? "—" : schedule.getLabel(),
                            payment.getAmountCents(),
                            payment.getMethod(),
                            payment.getReference(),
                            payment.getRecordedByUserId(),
                            payment.getPaidAt());
                })
                .toList();
    }

    private List<FeeSchedule> schedulesFor(Long schoolClassId) {
        return schoolClassId == null
                ? feeScheduleRepository.findAll()
                : feeScheduleRepository.findAllBySchoolClassId(schoolClassId);
    }

    /** Une seule requête pour tous les paiements, au lieu d'une par facture. */
    private Map<Long, Long> paidAmountsByInvoice(List<StudentFeeInvoice> invoices) {
        return paymentRepository.findAllByInvoiceIdIn(invoices.stream().map(StudentFeeInvoice::getId).toList()).stream()
                .collect(Collectors.groupingBy(
                        FeePayment::getInvoiceId, Collectors.summingLong(FeePayment::getAmountCents)));
    }

    private Map<Long, Student> studentsById(List<StudentFeeInvoice> invoices) {
        return studentRepository.findAllById(invoices.stream().map(StudentFeeInvoice::getStudentId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
    }

    private Map<Long, String> classNames() {
        return schoolClassRepository.findAll().stream()
                .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
    }

    private List<FeeSummaryResponse.CollectionByMethod> collectionByMethod(List<StudentFeeInvoice> invoices) {
        Map<FeePaymentMethod, List<FeePayment>> byMethod = paymentRepository
                .findAllByInvoiceIdIn(invoices.stream().map(StudentFeeInvoice::getId).toList()).stream()
                .collect(Collectors.groupingBy(FeePayment::getMethod));
        return byMethod.entrySet().stream()
                .map(entry -> new FeeSummaryResponse.CollectionByMethod(
                        entry.getKey(),
                        entry.getValue().stream().mapToLong(FeePayment::getAmountCents).sum(),
                        entry.getValue().size()))
                .sorted(Comparator.comparingLong(FeeSummaryResponse.CollectionByMethod::amountCents).reversed())
                .toList();
    }

    private FeeOutstandingEntry toOutstandingEntry(
            StudentFeeInvoice invoice,
            FeeSchedule schedule,
            Student student,
            long paidCents,
            Map<Long, String> classNames,
            LocalDate today) {
        LocalDate dueDate = schedule == null ? null : schedule.getDueDate();
        boolean overdue = isOverdue(schedule, today);
        return new FeeOutstandingEntry(
                invoice.getId(),
                invoice.getStudentId(),
                studentName(student),
                student == null ? "—" : student.getStudentNumber(),
                schedule == null ? null : schedule.getSchoolClassId(),
                schedule == null ? "—" : classNames.getOrDefault(schedule.getSchoolClassId(), "—"),
                schedule == null ? "—" : schedule.getLabel(),
                dueDate,
                invoice.getAmountDueCents(),
                paidCents,
                invoice.getAmountDueCents() - paidCents,
                invoice.getStatus(),
                overdue,
                overdue ? ChronoUnit.DAYS.between(dueDate, today) : 0);
    }

    private static boolean isOverdue(FeeSchedule schedule, LocalDate today) {
        return schedule != null && schedule.getDueDate().isBefore(today);
    }

    private static long remaining(StudentFeeInvoice invoice, Map<Long, Long> paidByInvoice) {
        return invoice.getAmountDueCents() - paidByInvoice.getOrDefault(invoice.getId(), 0L);
    }

    /** Une facture peut survivre à la suppression d'une fiche élève ; l'impayé reste à recouvrer. */
    private static String studentName(Student student) {
        return student == null ? "Élève supprimé" : student.getFirstName() + " " + student.getLastName();
    }
}
