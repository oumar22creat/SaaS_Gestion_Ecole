package com.schoolsaas.library;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Réservation d'un {@link Book} indisponible, en liste d'attente FIFO (cahier-des-charges.md §19.3). */
@Entity
@Table(name = "book_reservations")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class BookReservation extends TenantScopedEntity {

    @Column(name = "book_id", nullable = false, updatable = false)
    private Long bookId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookReservationStatus status;

    @Column(name = "reserved_at", nullable = false, updatable = false)
    private Instant reservedAt;

    protected BookReservation() {
    }

    public BookReservation(Long bookId, Long studentId) {
        this.bookId = bookId;
        this.studentId = studentId;
        this.status = BookReservationStatus.WAITING;
        this.reservedAt = Instant.now();
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public BookReservationStatus getStatus() {
        return status;
    }

    public void setStatus(BookReservationStatus status) {
        this.status = status;
    }

    public Instant getReservedAt() {
        return reservedAt;
    }
}
