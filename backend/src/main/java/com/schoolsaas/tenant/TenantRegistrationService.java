package com.schoolsaas.tenant;

import com.schoolsaas.auth.AuthService;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.TokenPairResponse;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import com.schoolsaas.tenant.dto.TenantRegistrationResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inscription en self-service d'un établissement (voir cahier-des-charges.md §20.1) : crée
 * le tenant et son compte Administrateur initial, puis connecte immédiatement ce dernier
 * ("activation immédiate").
 *
 * <p>L'assistant de configuration multi-étapes (§20.2 : import classes/matières, élèves,
 * enseignants, emploi du temps) n'est PAS implémenté ici — il dépend des modules Phase 1.5
 * (élèves/classes/matières) et 1.6 (emploi du temps), pas encore construits (voir
 * docs/ROADMAP.md et CLAUDE.md règle 5 : ne pas sauter de phase). Cette classe couvre la
 * première étape ("informations établissement"), le reste sera ajouté au fur et à mesure
 * que ces modules existeront.
 */
@Service
public class TenantRegistrationService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final TenantSessionConfigurer tenantSessionConfigurer;

    public TenantRegistrationService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthService authService,
            TenantSessionConfigurer tenantSessionConfigurer) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.tenantSessionConfigurer = tenantSessionConfigurer;
    }

    @Transactional
    public TenantRegistrationResponse register(TenantRegistrationRequest request) {
        if (tenantRepository.existsBySubdomain(request.subdomain())) {
            throw ApiException.conflict("SUBDOMAIN_ALREADY_TAKEN", "Ce sous-domaine est déjà utilisé");
        }

        Tenant tenant = tenantRepository.save(
                new Tenant(request.schoolName(), request.subdomain(), TenantStatus.TRIAL));

        User admin = new User(
                request.adminEmail(),
                passwordEncoder.encode(request.adminPassword()),
                request.adminFirstName(),
                request.adminLastName(),
                Role.ADMIN);
        admin.setSchoolId(tenant.getId());
        userRepository.save(admin);

        // Le contexte tenant n'est pas encore posé à ce stade (endpoint public, pas de JWT ni
        // d'en-tête X-Tenant-Id) : sans lui (et sans activer le filtre Hibernate), la
        // recherche par email dans login() ne serait pas bornée au tenant qu'on vient de créer
        // (voir com.schoolsaas.common.TenantScopedEntity et docs/ARCHITECTURE.md ADR-001).
        TenantContext.set(tenant.getId());
        AuthService.TokenPair tokens;
        try {
            tenantSessionConfigurer.applyTenant(tenant.getId());
            tokens = authService.login(request.adminEmail(), request.adminPassword());
        } finally {
            TenantContext.clear();
        }

        return new TenantRegistrationResponse(
                tenant.getId(),
                tenant.getSubdomain(),
                new TokenPairResponse(tokens.accessToken(), tokens.refreshToken(), tokens.expiresInSeconds()));
    }
}
