package com.schoolsaas.auth;

import com.schoolsaas.common.ApiException;
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

/** Connexion, rafraîchissement et déconnexion — voir docs/ARCHITECTURE.md ADR-002 et ADR-008. */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlatformAdminRepository platformAdminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            PlatformAdminRepository platformAdminRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.platformAdminRepository = platformAdminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenPair login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issueTokenPair(new AuthenticatedPrincipal(
                user.getId(), SubjectType.USER, user.getSchoolId(), user.getRole().name(), user.getEmail()));
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
        // findById cible une ligne unique par clé primaire, sans ambiguïté possible entre
        // tenants : pas besoin que le filtre Hibernate tenantFilter soit actif pour cette
        // recherche précise (l'id ne vient pas d'une entrée utilisateur, mais d'un refresh
        // token déjà validé par son hash).
        User user = userRepository.findById(token.getSubjectId())
                .filter(User::isActive)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Compte introuvable"));
        return new AuthenticatedPrincipal(user.getId(), SubjectType.USER, user.getSchoolId(), user.getRole().name(), user.getEmail());
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
