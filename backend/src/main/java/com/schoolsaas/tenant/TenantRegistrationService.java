package com.schoolsaas.tenant;

import com.schoolsaas.auth.AuthService;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.TokenPairResponse;
import com.schoolsaas.billing.SubscriptionService;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import com.schoolsaas.tenant.dto.TenantRegistrationResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscription en self-service d'un établissement (voir cahier-des-charges.md §20.1) : crée
 * le tenant, son compte Administrateur initial et son abonnement d'essai gratuit
 * (cahier-des-charges.md §4.2), puis connecte immédiatement l'Administrateur
 * ("activation immédiate").
 *
 * <p>L'assistant de configuration multi-étapes (§20.2 : import classes/matières, élèves,
 * enseignants, emploi du temps, <b>et choix explicite du plan</b>) n'est PAS implémenté ici
 * — il dépend des modules Phase 1.5 (élèves/classes/matières) et 1.6 (emploi du temps), pas
 * encore construits (voir docs/ROADMAP.md et CLAUDE.md règle 5 : ne pas sauter de phase).
 * En attendant, l'essai démarre automatiquement sur le plan par défaut (voir
 * {@link SubscriptionService#createTrialSubscription}) ; l'établissement pourra changer de
 * plan via {@code POST /api/v1/billing/checkout} une fois connecté.
 */
@Service
public class TenantRegistrationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final TenantSessionConfigurer tenantSessionConfigurer;
    private final SubscriptionService subscriptionService;

    public TenantRegistrationService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            TenantSessionConfigurer tenantSessionConfigurer,
            SubscriptionService subscriptionService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.tenantSessionConfigurer = tenantSessionConfigurer;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public TenantRegistrationResponse register(TenantRegistrationRequest request) {
        if (tenantRepository.existsBySubdomain(request.subdomain())) {
            throw ApiException.conflict("SUBDOMAIN_ALREADY_TAKEN", "Ce sous-domaine est déjà utilisé");
        }

        Tenant tenant = tenantRepository.save(
                new Tenant(request.schoolName(), request.subdomain(), TenantStatus.TRIAL));

        // Le contexte tenant n'est pas encore posé à ce stade (endpoint public, pas de JWT ni
        // d'en-tête X-Tenant-Id) : il doit être posé AVANT le premier INSERT dans `users`, pas
        // seulement avant login(). Piège découvert en testant l'inscription contre un vrai
        // Postgres avec le rôle applicatif restreint (pas le superutilisateur des tests
        // Testcontainers, qui contourne silencieusement RLS — voir ADR-001 Piège 4) : la
        // politique RLS sur `users` n'a pas de clause WITH CHECK explicite, donc Postgres
        // réutilise la clause USING (`school_id = current_setting('app.tenant_id', true)`)
        // comme condition d'insertion — sans `app.tenant_id` déjà positionné, l'INSERT de
        // l'Administrateur initial était rejeté par RLS avant même d'atteindre login().
        TenantContext.set(tenant.getId());
        AuthService.TokenPair tokens;
        try {
            tenantSessionConfigurer.applyTenant(tenant.getId());

            User admin = new User(
                    request.adminEmail(),
                    passwordEncoder.encode(request.adminPassword()),
                    request.adminFirstName(),
                    request.adminLastName(),
                    Role.ADMIN);
            admin.setSchoolId(tenant.getId());
            userRepository.save(admin);

            subscriptionService.createTrialSubscription(tenant.getId());

            tokens = authService.login(request.subdomain(), request.adminEmail(), request.adminPassword());
        } finally {
            TenantContext.clear();
        }

        return new TenantRegistrationResponse(
                tenant.getId(),
                tenant.getSubdomain(),
                new TokenPairResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
    }
}
