package com.schoolsaas.messaging.dto;

import java.util.List;

/**
 * {@code participantUserIds} est ignoré pour une annonce (diffusée à tous les utilisateurs
 * actifs du tenant, voir docs/ARCHITECTURE.md ADR-019) — obligatoire sinon.
 */
public record ConversationCreateRequest(String title, boolean announcement, List<Long> participantUserIds) {
}
