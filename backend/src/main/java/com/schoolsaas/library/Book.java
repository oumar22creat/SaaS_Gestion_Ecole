package com.schoolsaas.library;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Ouvrage du catalogue (cahier-des-charges.md §19.3). */
@Entity
@Table(name = "books")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Book extends TenantScopedEntity {

    @Column(nullable = false, updatable = false)
    private String barcode;

    @Column
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Book() {
    }

    public Book(String barcode, String isbn, String title, String author, int totalCopies) {
        this.barcode = barcode;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.createdAt = Instant.now();
    }

    public String getBarcode() {
        return barcode;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
