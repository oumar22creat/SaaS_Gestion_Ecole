package com.schoolsaas.mobilemoney;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.schoolfees.FeeInvoiceStatus;
import com.schoolsaas.schoolfees.FeePaymentMethod;
import com.schoolsaas.schoolfees.SchoolFeesService;
import com.schoolsaas.schoolfees.StudentFeeInvoice;
import com.schoolsaas.schoolfees.StudentFeeInvoiceRepository;
import com.schoolsaas.schoolfees.dto.FeePaymentCreateRequest;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantSessionConfigurer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Paiement des frais de scolarité par mobile money (cahier §4.3).
 *
 * <p>Deux points portent tout le risque de ce module :
 *
 * <ol>
 *   <li><b>Le contexte tenant.</b> Le callback de l'opérateur arrive sans jeton ni en-tête
 *       d'établissement. La référence transmise à l'opérateur porte donc l'identifiant du
 *       tenant, ce qui permet de poser le contexte <em>avant</em> la première lecture — sans
 *       quoi la politique RLS masque précisément la ligne recherchée.
 *   <li><b>L'idempotence.</b> Les opérateurs réessaient leurs callbacks. Une tentative déjà
 *       aboutie est donc ignorée sans rien recréditer : sans cela, une facture serait payée
 *       deux fois pour un seul encaissement réel.
 * </ol>
 */
@Service
public class MobileMoneyService {

    private static final Logger log = LoggerFactory.getLogger(MobileMoneyService.class);
    private static final String REFERENCE_PREFIX = "OM";

    private final MobileMoneyPaymentRepository paymentRepository;
    private final StudentFeeInvoiceRepository invoiceRepository;
    private final SchoolFeesService schoolFeesService;
    private final MobileMoneyGateway gateway;
    private final TenantSessionConfigurer tenantSessionConfigurer;

    public MobileMoneyService(
            MobileMoneyPaymentRepository paymentRepository,
            StudentFeeInvoiceRepository invoiceRepository,
            SchoolFeesService schoolFeesService,
            MobileMoneyGateway gateway,
            TenantSessionConfigurer tenantSessionConfigurer) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.schoolFeesService = schoolFeesService;
        this.gateway = gateway;
        this.tenantSessionConfigurer = tenantSessionConfigurer;
    }

    /** Demande un paiement pour une facture. Le montant vient de la facture, jamais du client. */
    @Transactional
    public MobileMoneyPayment initiate(Long invoiceId, String payerMsisdn) {
        StudentFeeInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> ApiException.notFound("INVOICE_NOT_FOUND", "Facture introuvable"));
        if (invoice.getStatus() == FeeInvoiceStatus.PAID) {
            throw ApiException.unprocessable("INVOICE_ALREADY_PAID", "Cette facture est déjà réglée");
        }
        if (invoice.getStatus() == FeeInvoiceStatus.CANCELLED) {
            throw ApiException.unprocessable("INVOICE_CANCELLED", "Cette facture est annulée");
        }

        long remaining = invoice.getAmountDueCents() - schoolFeesService.paidAmountForInvoice(invoiceId);
        if (remaining <= 0) {
            throw ApiException.unprocessable("NOTHING_LEFT_TO_PAY", "Cette facture n'a plus de solde à régler");
        }

        String reference = "%s-%d-%s"
                .formatted(REFERENCE_PREFIX, invoice.getSchoolId(), UUID.randomUUID().toString().substring(0, 12));

        MobileMoneyPayment payment =
                new MobileMoneyPayment(invoiceId, remaining, payerMsisdn, gateway.providerName(), reference);
        payment = paymentRepository.save(payment);

        gateway.initiate(reference, remaining, payerMsisdn, "Frais de scolarité");
        return payment;
    }

    /**
     * Traite le retour de l'opérateur. Appelé hors de toute session authentifiée : le tenant
     * est déduit de la référence, puis appliqué à la session avant toute lecture.
     */
    @Transactional
    public void handleCallback(String reference, boolean succeeded, String providerReference, String failureReason) {
        Long tenantId = tenantIdFrom(reference);
        TenantContext.set(tenantId);
        tenantSessionConfigurer.applyTenant(tenantId);

        MobileMoneyPayment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "Transaction introuvable"));

        if (payment.getStatus() != MobileMoneyStatus.PENDING) {
            // Rappel du fournisseur sur une transaction déjà tranchée : rien à refaire.
            log.info("Callback mobile money ignoré, transaction déjà {} : {}", payment.getStatus(), reference);
            return;
        }

        if (!succeeded) {
            payment.markFailed(failureReason);
            paymentRepository.save(payment);
            return;
        }

        payment.markSucceeded(providerReference);
        paymentRepository.save(payment);

        // Le montant crédité est celui de la transaction enregistrée, jamais celui annoncé par
        // le callback : ce dernier vient du réseau et ne doit pas pouvoir décider ce qu'on encaisse.
        schoolFeesService.recordPayment(
                payment.getInvoiceId(),
                new FeePaymentCreateRequest(payment.getAmountCents(), FeePaymentMethod.MOBILE_MONEY, reference),
                null);
    }

    /**
     * Identifiant d'établissement porté par la référence. Une référence forgée ne donne accès
     * à rien : la recherche qui suit s'exécute sous la politique RLS de ce tenant, donc une
     * référence inventée pour un autre établissement ne correspondra à aucune ligne.
     */
    private Long tenantIdFrom(String reference) {
        String[] parts = reference == null ? new String[0] : reference.split("-");
        if (parts.length < 3 || !REFERENCE_PREFIX.equals(parts[0])) {
            throw ApiException.unprocessable("INVALID_REFERENCE", "Référence de transaction invalide");
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw ApiException.unprocessable("INVALID_REFERENCE", "Référence de transaction invalide");
        }
    }
}
