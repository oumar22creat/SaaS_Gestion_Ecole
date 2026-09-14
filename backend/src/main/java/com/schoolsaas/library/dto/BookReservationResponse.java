package com.schoolsaas.library.dto;

import com.schoolsaas.library.BookReservation;
import com.schoolsaas.library.BookReservationStatus;
import java.time.Instant;

public record BookReservationResponse(
        Long id, Long bookId, Long studentId, BookReservationStatus status, Instant reservedAt, int position) {

    public static BookReservationResponse from(BookReservation reservation, int position) {
        return new BookReservationResponse(
                reservation.getId(), reservation.getBookId(), reservation.getStudentId(), reservation.getStatus(),
                reservation.getReservedAt(), position);
    }
}
