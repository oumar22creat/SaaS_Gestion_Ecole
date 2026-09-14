package com.schoolsaas.schoolfees;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolfees.dto.FeePaymentCreateRequest;
import com.schoolsaas.schoolfees.dto.FeeReportingResponse;
import com.schoolsaas.schoolfees.dto.FeeScheduleCreateRequest;
import com.schoolsaas.schoolfees.dto.StudentFeeInvoiceResponse;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Comptabilité et frais scolaires — cahier-des-charges.md §19.4, ROADMAP.md 3.3. */
@Service
public class SchoolFeesService {

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
                new FeeSchedule(request.schoolClassId(), request.label(), request.amountCents(), "XOF", request.dueDate()));
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
        FeePayment payment = paymentRepository.save(
                new FeePayment(invoiceId, request.amountCents(), request.method(), request.reference(), recordedByUserId));

        long totalPaid = paidAmountForInvoice(invoiceId);
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
}
