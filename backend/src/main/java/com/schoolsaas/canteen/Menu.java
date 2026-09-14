package com.schoolsaas.canteen;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Menu du jour, avec variante régime particulier optionnelle (cahier-des-charges.md §19.1). */
@Entity
@Table(name = "menus")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Menu extends TenantScopedEntity {

    @Column(nullable = false, updatable = false)
    private LocalDate date;

    @Column(name = "main_description", nullable = false)
    private String mainDescription;

    @Column(name = "special_diet_description")
    private String specialDietDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Menu() {
    }

    public Menu(LocalDate date, String mainDescription, String specialDietDescription) {
        this.date = date;
        this.mainDescription = mainDescription;
        this.specialDietDescription = specialDietDescription;
        this.createdAt = Instant.now();
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMainDescription() {
        return mainDescription;
    }

    public void setMainDescription(String mainDescription) {
        this.mainDescription = mainDescription;
    }

    public String getSpecialDietDescription() {
        return specialDietDescription;
    }

    public void setSpecialDietDescription(String specialDietDescription) {
        this.specialDietDescription = specialDietDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
