package com.schoolsaas.canteen;

import com.schoolsaas.canteen.dto.CanteenInvoiceGenerateRequest;
import com.schoolsaas.canteen.dto.CanteenPaymentCreateRequest;
import com.schoolsaas.canteen.dto.MealReservationCreateRequest;
import com.schoolsaas.canteen.dto.MenuCreateRequest;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.student.StudentRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cantine — cahier-des-charges.md §19.1, ROADMAP.md 3.4. */
@Service
public class CanteenService {

    private final MenuRepository menuRepository;
    private final MealReservationRepository reservationRepository;
    private final CanteenInvoiceRepository invoiceRepository;
    private final CanteenPaymentRepository paymentRepository;
    private final StudentRepository studentRepository;

    public CanteenService(
            MenuRepository menuRepository,
            MealReservationRepository reservationRepository,
            CanteenInvoiceRepository invoiceRepository,
            CanteenPaymentRepository paymentRepository,
            StudentRepository studentRepository) {
        this.menuRepository = menuRepository;
        this.reservationRepository = reservationRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public Menu upsertMenu(MenuCreateRequest request) {
        return menuRepository.findByDate(request.date())
                .map(existing -> {
                    existing.setMainDescription(request.mainDescription());
                    existing.setSpecialDietDescription(request.specialDietDescription());
                    return existing;
                })
                .orElseGet(() -> menuRepository.save(
                        new Menu(request.date(), request.mainDescription(), request.specialDietDescription())));
    }

    public List<Menu> listMenus(LocalDate from, LocalDate to) {
        return menuRepository.findAllByDateBetweenOrderByDate(from, to);
    }

    @Transactional
    public MealReservation reserveMeal(MealReservationCreateRequest request, Long staffUserId) {
        if (studentRepository.findById(request.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
        return reservationRepository.findByStudentIdAndDate(request.studentId(), request.date())
                .orElseGet(() -> reservationRepository.save(
                        new MealReservation(request.studentId(), request.date(), request.specialDiet(), staffUserId)));
    }

    public List<MealReservation> reservationsForStudent(Long studentId, LocalDate from, LocalDate to) {
        return reservationRepository.findAllByStudentIdAndDateBetween(studentId, from, to);
    }

    @Transactional
    public CanteenInvoice generateInvoice(CanteenInvoiceGenerateRequest request) {
        if (studentRepository.findById(request.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
        int mealCount = reservationsForStudent(request.studentId(), request.periodFrom(), request.periodTo()).size();
        return invoiceRepository.save(new CanteenInvoice(
                request.studentId(), request.periodFrom(), request.periodTo(), mealCount, request.pricePerMealCents()));
    }

    public CanteenInvoice getInvoice(Long id) {
        return invoiceRepository.findById(id).orElseThrow(() -> ApiException.notFound("INVOICE_NOT_FOUND", "Facture introuvable"));
    }

    public List<CanteenInvoice> invoicesForStudent(Long studentId) {
        return invoiceRepository.findAllByStudentIdOrderByIssuedAtDesc(studentId);
    }

    /** Suivi des impayés (cahier §19.1) : toutes les factures non réglées et non annulées, tous élèves. */
    public List<CanteenInvoice> listUnpaidInvoices() {
        return invoiceRepository.findAllByStatusNot(CanteenInvoiceStatus.PAID).stream()
                .filter(invoice -> invoice.getStatus() != CanteenInvoiceStatus.CANCELLED)
                .toList();
    }

    public long paidAmountForInvoice(Long invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId).stream().mapToLong(CanteenPayment::getAmountCents).sum();
    }

    public List<CanteenPayment> paymentsForInvoice(Long invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId);
    }

    @Transactional
    public CanteenPayment recordPayment(Long invoiceId, CanteenPaymentCreateRequest request, Long recordedByUserId) {
        CanteenInvoice invoice = getInvoice(invoiceId);
        if (invoice.getStatus() == CanteenInvoiceStatus.CANCELLED) {
            throw ApiException.badRequest("INVOICE_CANCELLED", "Cette facture est annulée", List.of());
        }
        CanteenPayment payment = paymentRepository.save(
                new CanteenPayment(invoiceId, request.amountCents(), request.method(), request.reference(), recordedByUserId));

        long totalPaid = paidAmountForInvoice(invoiceId);
        invoice.setStatus(
                totalPaid >= invoice.getAmountDueCents() ? CanteenInvoiceStatus.PAID
                        : totalPaid > 0 ? CanteenInvoiceStatus.PARTIALLY_PAID : CanteenInvoiceStatus.PENDING);
        return payment;
    }
}
