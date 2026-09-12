/**
 * Déclaration unique du filtre Hibernate global de scoping tenant (voir
 * {@link com.schoolsaas.common.TenantScopedEntity} et docs/ARCHITECTURE.md ADR-001).
 *
 * <p>Chaque entité scopée doit déclarer elle-même {@code @Filter(name = "tenantFilter",
 * condition = "school_id = :tenantId")} sur sa classe (l'héritage de cette annotation depuis
 * {@code TenantScopedEntity}, une {@code @MappedSuperclass}, n'est pas fiable en Hibernate —
 * voir la couverture de {@code TenantIsolationTest}).
 */
@org.hibernate.annotations.FilterDef(
        name = "tenantFilter",
        parameters = @org.hibernate.annotations.ParamDef(name = "tenantId", type = Long.class))
package com.schoolsaas.common;
