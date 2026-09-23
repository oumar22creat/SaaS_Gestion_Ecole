package com.schoolsaas.platformadmin;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.auth.PlatformAdmin;
import com.schoolsaas.auth.PlatformAdminRepository;
import com.schoolsaas.billing.Plan;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.Subscription;
import com.schoolsaas.billing.SubscriptionRepository;
import com.schoolsaas.billing.SubscriptionStatus;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.platformadmin.dto.CashPaymentRequest;
import com.schoolsaas.platformadmin.dto.PlanAdminResponse;
import com.schoolsaas.platformadmin.dto.PlanChangeRequest;
import com.schoolsaas.platformadmin.dto.PlatformAdminCreateRequest;
import com.schoolsaas.platformadmin.dto.TenantAdminResponse;
import com.schoolsaas.platformadmin.dto.TenantStatusUpdateRequest;
import com.schoolsaas.platformadmin.dto.TrialExtensionRequest;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Console plateforme (cahier-des-charges.md §5).
 *
 * <p>Le Super-Administrateur voit les établissements et leurs abonnements, jamais leurs
 * données scolaires : élèves, parents et comptes utilisateurs sont protégés par Row-Level
 * Security et lui restent invisibles (ADR-001). C'est une garantie vis-à-vis des
 * établissements clients, pas une limite du produit.
 *
 * <p>Toute action qui change la situation d'un client est journalisée avec son auteur et son
 * motif : couper l'accès d'une école entière ne doit jamais être un geste anonyme.
 */
@Service
public class PlatformAdminService {

    /** Le journal est relu par un humain : une date ISO brute y est illisible. */
    private static final java.time.format.DateTimeFormatter FRENCH_DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(java.time.ZoneOffset.UTC);

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final PlatformAdminActionRepository actionRepository;
    private final PasswordEncoder passwordEncoder;

    public PlatformAdminService(
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            PlatformAdminRepository platformAdminRepository,
            PlatformAdminActionRepository actionRepository,
            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.actionRepository = actionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ Établissements

    /** Liste des établissements, filtrable par statut et par recherche libre sur nom/sous-domaine. */
    public Page<TenantAdminResponse> listTenants(Pageable pageable, String search, TenantStatus status) {
        String term = search == null || search.isBlank() ? null : search.trim();
        Page<Tenant> page = selectTenants(pageable, term, status);
        Map<Long, Subscription> subscriptions = subscriptionsOf(page.getContent());
        Map<Long, Plan> plans = plansOf(subscriptions.values());
        return page.map(tenant -> toResponse(tenant, subscriptions.get(tenant.getId()), plans));
    }

    /** Les quatre combinaisons de critères, explicitement — voir TenantRepository. */
    private Page<Tenant> selectTenants(Pageable pageable, String term, TenantStatus status) {
        if (term == null && status == null) {
            return tenantRepository.findAll(pageable);
        }
        if (term == null) {
            return tenantRepository.findAllByStatus(status, pageable);
        }
        if (status == null) {
            return tenantRepository.search(term, pageable);
        }
        return tenantRepository.searchByStatus(term, status, pageable);
    }

    public TenantAdminResponse getTenant(Long tenantId) {
        Tenant tenant = requireTenant(tenantId);
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId).orElse(null);
        Map<Long, Plan> plans = plansOf(subscription == null ? List.of() : List.of(subscription));
        return toResponse(tenant, subscription, plans);
    }

    /**
     * Change le statut d'un établissement — suspension pour impayé, réactivation après
     * régularisation.
     *
     * <p>Un motif est exigé pour toute décision qui restreint l'accès : sans lui, le journal
     * dirait qu'une école a été coupée sans dire pourquoi, ce qui ne sert à personne.
     */
    @Transactional
    public TenantAdminResponse updateStatus(Long tenantId, TenantStatusUpdateRequest request) {
        Tenant tenant = requireTenant(tenantId);
        TenantStatus previous = tenant.getStatus();
        if (previous == request.status()) {
            return getTenant(tenantId);
        }
        boolean restricting = request.status() == TenantStatus.SUSPENDED
                || request.status() == TenantStatus.CANCELLED
                || request.status() == TenantStatus.READ_ONLY;
        if (restricting && (request.reason() == null || request.reason().isBlank())) {
            throw ApiException.unprocessable(
                    "REASON_REQUIRED", "Indiquez le motif : cette décision restreint l'accès de l'établissement");
        }

        tenant.setStatus(request.status());
        tenantRepository.save(tenant);
        log(tenantId, "TENANT_STATUS_CHANGED", previous + " → " + request.status(), request.reason());
        return getTenant(tenantId);
    }

    /**
     * Prolonge l'essai. Le nouveau terme part de la date d'échéance actuelle quand elle est
     * encore devant, et du jour même quand elle est passée : sans cela, prolonger un essai
     * déjà expiré de 15 jours l'aurait laissé expiré.
     */
    @Transactional
    public TenantAdminResponse extendTrial(Long tenantId, TrialExtensionRequest request) {
        requireTenant(tenantId);
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Aucun abonnement pour cet établissement"));

        Instant now = Instant.now();
        Instant base = subscription.getTrialEndsAt() != null && subscription.getTrialEndsAt().isAfter(now)
                ? subscription.getTrialEndsAt()
                : now;
        Instant newEnd = base.plus(request.days(), ChronoUnit.DAYS);
        subscription.setTrialEndsAt(newEnd);
        subscriptionRepository.save(subscription);

        // Un établissement suspendu pour essai expiré redevient utilisable : prolonger sans
        // rouvrir l'accès n'aurait aucun effet visible pour le client.
        Tenant tenant = requireTenant(tenantId);
        if (tenant.getStatus() == TenantStatus.SUSPENDED || tenant.getStatus() == TenantStatus.READ_ONLY) {
            tenant.setStatus(TenantStatus.TRIAL);
            tenantRepository.save(tenant);
        }

        log(tenantId, "TRIAL_EXTENDED", "+" + request.days() + " j → " + FRENCH_DATE.format(newEnd), request.reason());
        return getTenant(tenantId);
    }

    /**
     * Enregistre un règlement reçu en espèces : l'abonnement couvre à nouveau
     * l'établissement, et son accès rouvre.
     *
     * <p>La nouvelle échéance part de l'échéance en cours quand elle est encore devant —
     * une école qui règle en avance ne doit pas perdre les jours qu'elle a déjà payés — et
     * du jour même quand elle est passée : repartir d'une date échue vendrait des mois déjà
     * écoulés.
     *
     * <p>Le montant attendu est calculé ici plutôt que saisi : c'est le prix du plan
     * multiplié par la durée, et le journal doit pouvoir être relu comme un reçu. Si
     * l'établissement a payé autre chose (remise, arrangement), le motif est là pour le dire.
     */
    @Transactional
    public TenantAdminResponse recordCashPayment(Long tenantId, CashPaymentRequest request) {
        Tenant tenant = requireTenant(tenantId);
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plan introuvable"));
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Aucun abonnement pour cet établissement"));

        Instant now = Instant.now();
        Instant base = subscription.getCurrentPeriodEnd() != null && subscription.getCurrentPeriodEnd().isAfter(now)
                ? subscription.getCurrentPeriodEnd()
                : now;
        // Mois calendaires, pas des tranches de 30 jours : douze mois payés doivent ramener
        // à la même date l'année suivante, sinon l'abonnement annuel expire cinq jours trop tôt.
        Instant newEnd = base.atZone(ZoneOffset.UTC).plusMonths(request.months()).toInstant();

        subscription.setPlanId(plan.getId());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodEnd(newEnd);
        subscriptionRepository.save(subscription);

        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);

        long amount = (long) plan.getPriceCents() * request.months();
        String detail = plan.getCode() + " · " + request.months() + " mois · " + amount + " " + plan.getCurrency()
                + " → " + FRENCH_DATE.format(newEnd);
        log(tenantId, "CASH_PAYMENT_RECORDED", detail, request.reason());
        return getTenant(tenantId);
    }

    @Transactional
    public TenantAdminResponse changePlan(Long tenantId, PlanChangeRequest request) {
        requireTenant(tenantId);
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plan introuvable"));
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Aucun abonnement pour cet établissement"));

        String previousCode = planRepository.findById(subscription.getPlanId()).map(Plan::getCode).orElse("—");
        subscription.setPlanId(plan.getId());
        subscriptionRepository.save(subscription);
        log(tenantId, "PLAN_CHANGED", previousCode + " → " + plan.getCode(), request.reason());
        return getTenant(tenantId);
    }

    // ------------------------------------------------------------------------- Plans

    public List<PlanAdminResponse> listPlans() {
        Map<Long, Long> tenantsPerPlan = subscriptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(Subscription::getPlanId, Collectors.counting()));
        return planRepository.findAll().stream()
                .map(plan -> PlanAdminResponse.from(plan, tenantsPerPlan.getOrDefault(plan.getId(), 0L)))
                .toList();
    }

    // -------------------------------------------------------------- Comptes plateforme

    public List<PlatformAdmin> listAdmins() {
        return platformAdminRepository.findAll();
    }

    /**
     * Crée un second Super-Administrateur. Mot de passe d'au moins douze caractères : ce compte
     * voit tous les établissements de la plateforme, il n'a pas le même profil de risque qu'un
     * compte d'établissement.
     */
    @Transactional
    public PlatformAdmin createAdmin(PlatformAdminCreateRequest request) {
        if (platformAdminRepository.findByEmail(request.email()).isPresent()) {
            throw ApiException.conflict("EMAIL_ALREADY_USED", "Un compte plateforme utilise déjà cet e-mail");
        }
        PlatformAdmin admin = platformAdminRepository.save(new PlatformAdmin(
                request.email(), passwordEncoder.encode(request.password()), request.firstName(), request.lastName()));
        log(null, "PLATFORM_ADMIN_CREATED", request.email(), null);
        return admin;
    }

    // ------------------------------------------------------------------------ Journal

    public Page<PlatformAdminAction> listActions(Pageable pageable) {
        return actionRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<PlatformAdminAction> actionsForTenant(Long tenantId) {
        return actionRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    /**
     * Noms des établissements cités par une liste d'actions. Le journal stocke un identifiant ;
     * on le relit avec un nom, faute de quoi « #9 » n'apprend rien à personne.
     */
    public Map<Long, String> tenantNamesOf(List<PlatformAdminAction> actions) {
        List<Long> ids = actions.stream()
                .map(PlatformAdminAction::getTenantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return ids.isEmpty()
                ? Map.of()
                : tenantRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Tenant::getId, Tenant::getName));
    }

    private void log(Long tenantId, String action, String detail, String reason) {
        actionRepository.save(new PlatformAdminAction(currentAdminId(), tenantId, action, detail, reason));
    }

    private static Long currentAdminId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .subjectId();
    }

    private Tenant requireTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
    }

    private Map<Long, Subscription> subscriptionsOf(List<Tenant> tenants) {
        return tenants.stream()
                .map(tenant -> subscriptionRepository.findByTenantId(tenant.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(Subscription::getTenantId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Plan> plansOf(java.util.Collection<Subscription> subscriptions) {
        return planRepository
                .findAllById(subscriptions.stream().map(Subscription::getPlanId).distinct().toList()).stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));
    }

    private static TenantAdminResponse toResponse(Tenant tenant, Subscription subscription, Map<Long, Plan> plans) {
        Plan plan = subscription == null ? null : plans.get(subscription.getPlanId());
        return new TenantAdminResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getCustomDomain(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                plan == null ? null : plan.getCode(),
                plan == null ? null : plan.getName(),
                plan == null ? null : plan.getPriceCents(),
                plan == null ? null : plan.getCurrency(),
                subscription == null ? null : subscription.getStatus(),
                subscription == null ? null : subscription.getTrialEndsAt(),
                subscription == null ? null : subscription.getCurrentPeriodEnd(),
                subscription == null ? null : subscription.getPaymentFailedAt());
    }
}
