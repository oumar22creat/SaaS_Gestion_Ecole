package com.schoolsaas.tenant;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Résout le tenant courant à partir d'une requête HTTP, pour les cas où il n'a pas déjà été
 * déduit du JWT (endpoints publics testés avec l'en-tête X-Tenant-Id, ou résolution par
 * sous-domaine — voir docs/ARCHITECTURE.md ADR-005 et README.md "Multi-tenant en local").
 */
@Component
public class TenantResolver {

    public static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantRepository tenantRepository;

    public TenantResolver(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Optional<Tenant> resolve(HttpServletRequest request) {
        Optional<Tenant> byHeader = resolveByHeader(request);
        if (byHeader.isPresent()) {
            return byHeader;
        }
        return resolveBySubdomain(request);
    }

    private Optional<Tenant> resolveByHeader(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (header == null || header.isBlank()) {
            return Optional.empty();
        }
        try {
            return tenantRepository.findById(Long.parseLong(header.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private Optional<Tenant> resolveBySubdomain(HttpServletRequest request) {
        String host = request.getServerName();
        if (host == null) {
            return Optional.empty();
        }
        String[] parts = host.split("\\.");
        if (parts.length < 3) {
            // ex. "localhost" ou "monapp.com" : pas de sous-domaine établissement.
            return Optional.empty();
        }
        String subdomain = parts[0];
        return tenantRepository.findBySubdomain(subdomain);
    }
}
