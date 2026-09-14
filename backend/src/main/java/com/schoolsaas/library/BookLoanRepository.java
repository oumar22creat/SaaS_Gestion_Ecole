package com.schoolsaas.library;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookLoanRepository extends JpaRepository<BookLoan, Long> {

    List<BookLoan> findAllByBookIdAndReturnedAtIsNull(Long bookId);

    List<BookLoan> findAllByStudentIdOrderByBorrowedAtDesc(Long studentId);

    List<BookLoan> findAllByReturnedAtIsNull();
}
