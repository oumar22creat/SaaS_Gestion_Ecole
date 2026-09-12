package com.schoolsaas.common;

import com.schoolsaas.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/**
 * Classe de base pour toute entité métier scopée à un établissement (voir CLAUDE.md règle 1
 * et docs/ARCHITECTURE.md ADR-001).
 *
 * <p><b>Important</b> : chaque sous-classe concrète doit déclarer elle-même
 * {@code @Filter(name = "tenantFilter", condition = "school_id = :tenantId")} sur SA classe
 * (voir package-info.java de ce package pour la définition unique de {@code @FilterDef}).
 * L'annotation {@code @Filter} placée sur une {@code @MappedSuperclass} n'est PAS fiablement
 * héritée par Hibernate — {@code TenantIsolationTest} a mis ce piège en évidence : le filtre
 * défini seulement ici n'avait aucun effet sur les entités concrètes.
 */
@MappedSuperclass
public abstract class TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "school_id", nullable = false, updatable = false)
    private Long schoolId;

    public Long getId() {
        return id;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    /**
     * Ne fait confiance à aucune valeur de school_id fournie par le client (voir
     * docs/API_CONVENTIONS.md) : si elle n'a pas déjà été positionnée explicitement, elle est
     * déduite du contexte tenant de la requête courante au moment de la persistance.
     */
    @PrePersist
    protected void assignTenantIfMissing() {
        if (this.schoolId == null) {
            this.schoolId = TenantContext.get();
        }
    }
}
