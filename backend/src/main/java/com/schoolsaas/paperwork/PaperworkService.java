package com.schoolsaas.paperwork;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.schoolfees.FeePayment;
import com.schoolsaas.schoolfees.FeePaymentRepository;
import com.schoolsaas.schoolfees.FeeSchedule;
import com.schoolsaas.schoolfees.FeeScheduleRepository;
import com.schoolsaas.schoolfees.StudentFeeInvoice;
import com.schoolsaas.schoolfees.StudentFeeInvoiceRepository;
import com.schoolsaas.schoolyear.SchoolYear;
import com.schoolsaas.schoolyear.SchoolYearService;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Documents officiels remis au guichet (cahier-des-charges.md §7/§19.4).
 *
 * <p>Ce sont les papiers qu'un secrétariat imprime tous les jours et que l'application ne
 * savait pas produire : le certificat de scolarité, réclamé pour une bourse, une demande de
 * visa ou une inscription ailleurs, et le reçu de règlement, que toute famille demande après
 * avoir payé en espèces.
 */
@Service
public class PaperworkService {

    /** Lieu porté sur les documents tant que l'établissement n'a pas saisi le sien. */
    private static final String DEFAULT_PLACE = "Bamako";

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolYearService schoolYearService;
    private final StudentFeeInvoiceRepository invoiceRepository;
    private final FeeScheduleRepository feeScheduleRepository;
    private final FeePaymentRepository feePaymentRepository;
    private final TenantRepository tenantRepository;
    private final SchoolDocumentPdfWriter pdfWriter;

    public PaperworkService(
            StudentRepository studentRepository,
            SchoolClassRepository schoolClassRepository,
            SchoolYearService schoolYearService,
            StudentFeeInvoiceRepository invoiceRepository,
            FeeScheduleRepository feeScheduleRepository,
            FeePaymentRepository feePaymentRepository,
            TenantRepository tenantRepository,
            SchoolDocumentPdfWriter pdfWriter) {
        this.studentRepository = studentRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolYearService = schoolYearService;
        this.invoiceRepository = invoiceRepository;
        this.feeScheduleRepository = feeScheduleRepository;
        this.feePaymentRepository = feePaymentRepository;
        this.tenantRepository = tenantRepository;
        this.pdfWriter = pdfWriter;
    }

    /**
     * Certificat de scolarité : atteste qu'un élève est bien inscrit, pour cette année, dans
     * cette classe. Refusé si l'élève est désactivé — un certificat est une attestation, pas
     * un historique, et le délivrer pour quelqu'un qui a quitté l'établissement serait faux.
     */
    public byte[] enrollmentCertificate(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
        if (!student.isActive()) {
            throw ApiException.unprocessable(
                    "STUDENT_NOT_ACTIVE", "Impossible d'attester la scolarité d'un élève désactivé");
        }
        SchoolYear year = schoolYearService.requireActive();
        String className = student.getSchoolClassId() == null
                ? null
                : schoolClassRepository.findById(student.getSchoolClassId()).map(SchoolClass::getName).orElse(null);
        if (className == null) {
            throw ApiException.unprocessable(
                    "STUDENT_WITHOUT_CLASS", "Affectez l'élève à une classe avant d'établir son certificat");
        }

        Tenant tenant = currentTenant();
        List<String> body = new ArrayList<>();
        body.add("Je soussigné, responsable de l'établissement " + tenant.getName() + ", certifie que :");
        body.add("");
        body.add("Nom et prénom : " + student.getLastName().toUpperCase(Locale.FRENCH) + " " + student.getFirstName());
        body.add("Matricule : " + student.getStudentNumber());
        if (student.getBirthDate() != null) {
            body.add("Né(e) le : " + student.getBirthDate().format(SchoolDocumentPdfWriter.FRENCH_DATE));
        }
        body.add("");
        body.add("est régulièrement inscrit(e) dans notre établissement pour l'année scolaire");
        body.add(year.getLabel() + ", en classe de " + className + ".");
        body.add("");
        body.add("Le présent certificat est délivré à l'intéressé(e) pour servir et valoir ce que de droit.");

        return pdfWriter.write(
                tenant,
                "CERTIFICAT DE SCOLARITÉ",
                body,
                SchoolDocumentPdfWriter.issuedAt(DEFAULT_PLACE, LocalDate.now()));
    }

    /**
     * Reçu d'un règlement encaissé. Reprend le montant enregistré et non un montant transmis
     * par l'appelant : un reçu qui n'est pas la copie exacte de l'écriture comptable ne vaut
     * rien.
     */
    public byte[] paymentReceipt(Long paymentId) {
        FeePayment payment = feePaymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Règlement introuvable"));
        StudentFeeInvoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> ApiException.notFound("INVOICE_NOT_FOUND", "Facture introuvable"));
        Student student = studentRepository.findById(invoice.getStudentId())
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable"));
        FeeSchedule schedule = feeScheduleRepository.findById(invoice.getFeeScheduleId()).orElse(null);

        long alreadyPaid = feePaymentRepository.findAllByInvoiceId(invoice.getId()).stream()
                .mapToLong(FeePayment::getAmountCents)
                .sum();
        long remaining = invoice.getAmountDueCents() - alreadyPaid;
        Tenant tenant = currentTenant();

        List<String> body = new ArrayList<>();
        body.add("Reçu n° " + payment.getId());
        body.add("Date du règlement : "
                + payment.getPaidAt().atZone(ZoneId.systemDefault()).toLocalDate().format(SchoolDocumentPdfWriter.FRENCH_DATE));
        body.add("");
        body.add("Reçu de : " + student.getLastName().toUpperCase(Locale.FRENCH) + " " + student.getFirstName()
                + " (" + student.getStudentNumber() + ")");
        body.add("Au titre de : " + (schedule == null ? "frais de scolarité" : schedule.getLabel()));
        body.add("Moyen de paiement : " + methodLabel(payment.getMethod().name()));
        if (payment.getReference() != null && !payment.getReference().isBlank()) {
            body.add("Référence : " + payment.getReference());
        }
        body.add("");
        body.add("Montant reçu : " + money(payment.getAmountCents()));
        body.add("Total versé sur cette échéance : " + money(alreadyPaid) + " sur " + money(invoice.getAmountDueCents()));
        body.add(remaining > 0 ? "Reste à payer : " + money(remaining) : "Échéance soldée.");

        return pdfWriter.write(
                tenant,
                "REÇU DE RÈGLEMENT",
                body,
                SchoolDocumentPdfWriter.issuedAt(DEFAULT_PLACE, LocalDate.now()));
    }

    private Tenant currentTenant() {
        return tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
    }

    /**
     * XOF : devise sans sous-unité, les montants « cents » sont des francs entiers.
     *
     * <p>Le séparateur de milliers français produit par Java est une espace fine insécable
     * (U+202F) que la police standard du PDF ne sait pas encoder : tout reçu d'au moins
     * 1 000 F échouait à la génération. On normalise vers une espace ordinaire.
     */
    private static String money(long amountCents) {
        String formatted = NumberFormat.getIntegerInstance(Locale.FRANCE).format(amountCents);
        return formatted.replace('\u202f', ' ').replace('\u00a0', ' ') + " XOF";
    }

    private static String methodLabel(String method) {
        return switch (method) {
            case "CASH" -> "Espèces";
            case "MOBILE_MONEY" -> "Mobile Money";
            case "BANK_TRANSFER" -> "Virement bancaire";
            default -> "Autre";
        };
    }
}
