package com.schoolsaas.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Génère et valide les access tokens JWT (voir docs/ARCHITECTURE.md ADR-002). */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(principal.subjectId()))
                .claim("subjectType", principal.subjectType().name())
                .claim("role", principal.role())
                .claim("email", principal.email())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(properties.accessTokenTtlMinutes()))))
                .signWith(signingKey);
        if (principal.tenantId() != null) {
            builder.claim("tenantId", principal.tenantId());
        }
        return builder.compact();
    }

    public Optional<AuthenticatedPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            Number tenantIdClaim = claims.get("tenantId", Number.class);
            Long tenantId = tenantIdClaim != null ? tenantIdClaim.longValue() : null;
            return Optional.of(new AuthenticatedPrincipal(
                    Long.parseLong(claims.getSubject()),
                    SubjectType.valueOf(claims.get("subjectType", String.class)),
                    tenantId,
                    claims.get("role", String.class),
                    claims.get("email", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long accessTokenTtlSeconds() {
        return Duration.ofMinutes(properties.accessTokenTtlMinutes()).toSeconds();
    }

    public long refreshTokenTtlDays() {
        return properties.refreshTokenTtlDays();
    }
}
