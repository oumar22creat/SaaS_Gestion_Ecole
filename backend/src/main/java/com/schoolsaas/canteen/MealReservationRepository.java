package com.schoolsaas.canteen;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealReservationRepository extends JpaRepository<MealReservation, Long> {

    Optional<MealReservation> findByStudentIdAndDate(Long studentId, LocalDate date);

    List<MealReservation> findAllByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);

    List<MealReservation> findAllByDate(LocalDate date);
}
