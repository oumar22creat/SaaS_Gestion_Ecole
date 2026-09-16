package com.schoolsaas.auth;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantSessionConfigurer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Connexion, rafraîchissement et déconnexion — voir docs/ARCHITECTURE.md ADR-002, ADR-008 et ADR-029. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantRepository tenantRepository;
    private final TenantSessionConfigurer tenantSessionConfigurer;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PlatformAdminRepository platformAdminRepository,
            RefreshTokenRepository refreshTokenRepository,
            TenantRepository tenantRepository,
            TenantSessionConfigurer tenantSessionConfigurer,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSessionConfigurer = tenantSessionConfigurer;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * L'e-mail n'est unique que par établissement (cahier §21), et {@code users} impose Row-
     * Level Security (voir V2__create_users_table.sql) : il faut donc résoudre et appliquer le
     * tenant AVANT de chercher l'utilisateur par e-mail, comme le fait déjà
     * {@link com.schoolsaas.tenant.TenantRegistrationService#register} pour son premier INSERT
     * — voir docs/ARCHITECTURE.md ADR-029 pour le bug corrigé ici (dépendait auparavant d'un
     * contexte tenant laissé par une requête précédente sur la même connexion poolée).
     */
    @Transactional
    public TokenPair login(String subdomain, String email, String password) {
        Tenant tenant = tenantRepository.findBySubdomain(subdomain).orElseThrow(AuthService::invalidCredentials);
        TenantContext.set(tenant.getId());
        try {
            tenantSessionConfigurer.applyTenant(tenant.getId());
            User user = userRepository.findByEmail(email)
                    .filter(User::isActive)
                    .orElseThrow(AuthService::invalidCredentials);
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw invalidCredentials();
            }
            return issueTokenPair(new AuthenticatedPrincipal(
                    user.getId(), SubjectType.USER, user.getSchoolId(), user.getRole().name(), user.getEmail()));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    public TokenPair adminLogin(String email, String password) {
        PlatformAdmin admin = platformAdminRepository.findByEmail(email)
                .orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokenPair(new AuthenticatedPrincipal(
                admin.getId(), SubjectType.PLATFORM_ADMIN, null, "SUPER_ADMIN", admin.getEmail()));
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .filter(RefreshToken::isUsable)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Jeton de rafraîchissement invalide ou expiré"));
        existing.revoke();
        return issueTokenPair(resolvePrincipal(existing));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    private AuthenticatedPrincipal resolvePrincipal(RefreshToken token) {
        if (token.getSubjectType() == SubjectType.PLATFORM_ADMIN) {
            PlatformAdmin admin = platformAdminRepository.findById(token.getSubjectId())
                    .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Compte introuvable"));
            return new AuthenticatedPrincipal(admin.getId(), SubjectType.PLATFORM_ADMIN, null, "SUPER_ADMIN", admin.getEmail());
        }
        // findById cible une ligne unique par clé primaire (pas besoin du filtre Hibernate
        // tenantFilter pour lever une ambiguïté, l'id ne vient pas d'une entrée utilisateur
        // mais d'un refresh token déjà validé par son hash) — MAIS la politique Row-Level
        // Security sur `users` (FORCE ROW LEVEL SECURITY) s'applique quand même, y compris à
        // une recherche par clé primaire : sans positionner app.tenant_id, la ligne reste
        // invisible. Voir docs/ARCHITECTURE.md ADR-029 (même piège que login()) — tenantId
        // vient ici du refresh token lui-même (refresh_tokens n'est pas scopé/RLS, voir
        // V3__create_platform_admins_and_refresh_tokens.sql), pas d'un e-mail fourni par le
        // client.
        TenantContext.set(token.getTenantId());
        try {
            tenantSessionConfigurer.applyTenant(token.getTenantId());
            User user = userRepository.findById(token.getSubjectId())
                    .filter(User::isActive)
                    .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Compte introuvable"));
            return new AuthenticatedPrincipal(user.getId(), SubjectType.USER, user.getSchoolId(), user.getRole().name(), user.getEmail());
        } finally {
            TenantContext.clear();
        }
    }

    private TokenPair issueTokenPair(AuthenticatedPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = generateRawToken();
        Instant expiresAt = Instant.now().plus(Duration.ofDays(jwtService.refreshTokenTtlDays()));
        refreshTokenRepository.save(new RefreshToken(
                hash(rawRefreshToken), principal.subjectType(), principal.subjectId(), principal.tenantId(), expiresAt));
        return new TokenPair(accessToken, rawRefreshToken, jwtService.accessTokenTtlSeconds());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("INVALID_CREDENTIALS", "Identifiants invalides");
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
    }
}
