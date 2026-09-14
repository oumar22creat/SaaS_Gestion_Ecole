package com.schoolsaas.canteen;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/**
 * Réservation d'un repas pour un élève à une date (cahier-des-charges.md §19.1). Saisie par le
 * personnel pour cette passe — pas de portail parent (ADR-010), voir ADR-025.
 */
@Entity
@Table(name = "meal_reservations")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class MealReservation extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(nullable = false, updatable = false)
    private LocalDate date;

    @Column(name = "special_diet", nullable = false)
    private boolean specialDiet;

    @Column(name = "reserved_by_user_id", nullable = false, updatable = false)
    private Long reservedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MealReservation() {
    }

    public MealReservation(Long studentId, LocalDate date, boolean specialDiet, Long reservedByUserId) {
        this.studentId = studentId;
        this.date = date;
        this.specialDiet = specialDiet;
        this.reservedByUserId = reservedByUserId;
        this.createdAt = Instant.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isSpecialDiet() {
        return specialDiet;
    }

    public Long getReservedByUserId() {
        return reservedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
