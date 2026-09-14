package com.schoolsaas.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Un plan d'abonnement du catalogue (cahier-des-charges.md §4.1). Entité plateforme, pas
 * scopée par tenant (voir docs/DATA_MODEL.md et docs/ARCHITECTURE.md ADR-009).
 */
@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(nullable = false)
    private String currency;

    @Column(name = "max_students")
    private Integer maxStudents;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Plan() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getPriceCents() {
        return priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    /**
     * Positionné une fois le prix récurrent créé côté dashboard Stripe pour ce plan (aucun
     * back-office Super-Admin pour l'automatiser encore, voir docs/ARCHITECTURE.md ADR-009
     * — Phase 2). Tant que null, le plan reste listable mais pas achetable en ligne, voir
     * BillingCheckoutService.
     */
    public void setStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
