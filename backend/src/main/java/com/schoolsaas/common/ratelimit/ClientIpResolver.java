package com.schoolsaas.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Adresse de l'appelant pour la limitation de débit.
 *
 * <p>En production l'API est derrière un reverse proxy : {@code getRemoteAddr()} renverrait
 * l'adresse du proxy, donc un compteur unique partagé par tous les utilisateurs. On lit donc
 * {@code X-Forwarded-For} en priorité, en ne gardant que la première entrée, la seule que le
 * proxy de tête a réellement observée.
 *
 * <p>À noter : cet en-tête est falsifiable si l'API est exposée directement. La limitation
 * par compte, elle, ne dépend pas de l'adresse et reste efficace dans ce cas.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
