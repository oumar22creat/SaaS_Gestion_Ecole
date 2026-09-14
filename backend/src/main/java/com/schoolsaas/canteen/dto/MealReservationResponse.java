package com.schoolsaas.canteen.dto;

import com.schoolsaas.canteen.MealReservation;
import java.time.LocalDate;

public record MealReservationResponse(Long id, Long studentId, LocalDate date, boolean specialDiet, Long reservedByUserId) {

    public static MealReservationResponse from(MealReservation reservation) {
        return new MealReservationResponse(
                reservation.getId(), reservation.getStudentId(), reservation.getDate(), reservation.isSpecialDiet(),
                reservation.getReservedByUserId());
    }
}
