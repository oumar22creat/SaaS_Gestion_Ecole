package com.schoolsaas.library.dto;

import com.schoolsaas.library.BookLoan;
import java.time.Instant;
import java.time.LocalDate;

public record BookLoanResponse(
        Long id, Long bookId, Long studentId, LocalDate borrowedAt, LocalDate dueDate, Instant returnedAt, boolean overdue) {

    public static BookLoanResponse from(BookLoan loan, LocalDate today) {
        return new BookLoanResponse(
                loan.getId(), loan.getBookId(), loan.getStudentId(), loan.getBorrowedAt(), loan.getDueDate(),
                loan.getReturnedAt(), loan.isOverdue(today));
    }
}
