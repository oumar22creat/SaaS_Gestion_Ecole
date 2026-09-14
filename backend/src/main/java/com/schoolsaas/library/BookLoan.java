package com.schoolsaas.library;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Emprunt d'un {@link Book} par un élève (cahier-des-charges.md §19.3). */
@Entity
@Table(name = "book_loans")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class BookLoan extends TenantScopedEntity {

    @Column(name = "book_id", nullable = false, updatable = false)
    private Long bookId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private LocalDate borrowedAt;

    @Column(name = "due_date", nullable = false, updatable = false)
    private LocalDate dueDate;

    @Column(name = "returned_at")
    private Instant returnedAt;

    protected BookLoan() {
    }

    public BookLoan(Long bookId, Long studentId, LocalDate borrowedAt, LocalDate dueDate) {
        this.bookId = bookId;
        this.studentId = studentId;
        this.borrowedAt = borrowedAt;
        this.dueDate = dueDate;
    }

    public Long getBookId() {
        return bookId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public LocalDate getBorrowedAt() {
        return borrowedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public void markReturned(Instant returnedAt) {
        this.returnedAt = returnedAt;
    }

    public boolean isActive() {
        return returnedAt == null;
    }

    public boolean isOverdue(LocalDate today) {
        return isActive() && dueDate.isBefore(today);
    }
}
