package com.schoolsaas.library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookReservationRepository extends JpaRepository<BookReservation, Long> {

    List<BookReservation> findAllByBookIdAndStatusOrderByReservedAtAsc(Long bookId, BookReservationStatus status);

    Optional<BookReservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(Long bookId, BookReservationStatus status);
}
