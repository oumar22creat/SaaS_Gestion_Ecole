package com.schoolsaas.library;

import com.schoolsaas.notification.NotificationDispatcher;
import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantSessionConfigurer;
import com.schoolsaas.tenant.TenantStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.DateTimeFormatter;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.notification.FamilyRecipients;

/**
 * Relances automatiques pour les emprunts en retard (cahier-des-charges.md §19.3). Aucun
 * compte élève/parent (ADR-010) : la "relance" est une notification loggée via le registre
 * central (ADR-020), pas un e-mail/SMS envoyé à une famille — voir ADR-026.
 *
 * <p>Parcourt chaque tenant actif l'un après l'autre en activant explicitement son contexte
 * ({@link TenantContext}/{@link TenantSessionConfigurer}), exactement comme le fait une
 * requête HTTP normale : c'est la seule façon sûre d'interroger une table métier protégée par
 * RLS depuis un job qui doit couvrir tous les tenants, sans jamais contourner RLS (voir
 * docs/ARCHITECTURE.md ADR-023, "Points ouverts").
 */
@Component
public class LibraryOverdueReminderJob {

    private final TenantRepository tenantRepository;
    private final TenantSessionConfigurer tenantSessionConfigurer;
    private final BookLoanRepository bookLoanRepository;
    private final NotificationDispatcher notificationDispatcher;
    private final FamilyRecipients familyRecipients;
    private final StudentRepository studentRepository;
    private final BookRepository bookRepository;

    /** Les familles lisent une date, pas un format ISO. */
    private static final DateTimeFormatter JOUR_MOIS_ANNEE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public LibraryOverdueReminderJob(
            TenantRepository tenantRepository,
            TenantSessionConfigurer tenantSessionConfigurer,
            BookLoanRepository bookLoanRepository,
            NotificationDispatcher notificationDispatcher,
            FamilyRecipients familyRecipients,
            StudentRepository studentRepository,
            BookRepository bookRepository) {
        this.tenantRepository = tenantRepository;
        this.tenantSessionConfigurer = tenantSessionConfigurer;
        this.bookLoanRepository = bookLoanRepository;
        this.notificationDispatcher = notificationDispatcher;
        this.familyRecipients = familyRecipients;
        this.studentRepository = studentRepository;
        this.bookRepository = bookRepository;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void run() {
        runFor(LocalDate.now());
    }

    public void runFor(LocalDate today) {
        List<Tenant> operatingTenants = tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getStatus() == TenantStatus.TRIAL
                        || tenant.getStatus() == TenantStatus.ACTIVE
                        || tenant.getStatus() == TenantStatus.READ_ONLY)
                .toList();
        for (Tenant tenant : operatingTenants) {
            remindOverdueLoansFor(tenant.getId(), today);
        }
    }

    @Transactional
    public void remindOverdueLoansFor(Long tenantId, LocalDate today) {
        TenantContext.set(tenantId);
        try {
            tenantSessionConfigurer.applyTenant(tenantId);
            for (BookLoan loan : bookLoanRepository.findAllByReturnedAtIsNull()) {
                if (loan.isOverdue(today)) {
                    notificationDispatcher.dispatch(
                            NotificationType.LIBRARY_OVERDUE,
                            familyRecipients.familleDe(loan.getStudentId()),
                            "Retard de bibliothèque",
                            corpsDuRappel(loan));
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Le texte qu'une famille recevra.
     *
     * <p>Il portait des identifiants techniques — « Élève 5 — ouvrage 3 ». Personne ne le
     * recevait, la liste de destinataires étant vide, ce qui masquait le défaut : le jour où
     * l'envoi fonctionne, un parent doit reconnaître son enfant et le livre à rapporter.
     */
    private String corpsDuRappel(BookLoan loan) {
        String eleve = studentRepository
                .findById(loan.getStudentId())
                .map(e -> e.getFirstName() + " " + e.getLastName())
                .orElse("Votre enfant");
        String ouvrage = bookRepository.findById(loan.getBookId()).map(Book::getTitle).orElse("un ouvrage");
        return "%s — « %s » était dû le %s".formatted(eleve, ouvrage, loan.getDueDate().format(JOUR_MOIS_ANNEE));
    }
}
