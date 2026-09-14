package com.schoolsaas.library;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.library.dto.BookCreateRequest;
import com.schoolsaas.library.dto.BookLoanCreateRequest;
import com.schoolsaas.library.dto.BookReservationCreateRequest;
import com.schoolsaas.student.StudentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bibliothèque — cahier-des-charges.md §19.3, ROADMAP.md 3.5. */
@Service
public class LibraryService {

    private final BookRepository bookRepository;
    private final BookLoanRepository bookLoanRepository;
    private final BookReservationRepository bookReservationRepository;
    private final StudentRepository studentRepository;

    public LibraryService(
            BookRepository bookRepository,
            BookLoanRepository bookLoanRepository,
            BookReservationRepository bookReservationRepository,
            StudentRepository studentRepository) {
        this.bookRepository = bookRepository;
        this.bookLoanRepository = bookLoanRepository;
        this.bookReservationRepository = bookReservationRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public Book createBook(BookCreateRequest request) {
        if (bookRepository.findByBarcode(request.barcode()).isPresent()) {
            throw ApiException.conflict("BARCODE_ALREADY_USED", "Ce code-barres est déjà utilisé");
        }
        return bookRepository.save(new Book(request.barcode(), request.isbn(), request.title(), request.author(), request.totalCopies()));
    }

    public List<Book> search(String query) {
        return bookRepository.findAllByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
                query, query, query);
    }

    public Book getBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> ApiException.notFound("BOOK_NOT_FOUND", "Ouvrage introuvable"));
    }

    public int availableCopies(Long bookId) {
        Book book = getBook(bookId);
        return book.getTotalCopies() - bookLoanRepository.findAllByBookIdAndReturnedAtIsNull(bookId).size();
    }

    @Transactional
    public BookLoan borrowBook(Long bookId, BookLoanCreateRequest request) {
        if (studentRepository.findById(request.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
        if (availableCopies(bookId) <= 0) {
            throw ApiException.conflict("NO_COPIES_AVAILABLE", "Aucun exemplaire disponible — réserver l'ouvrage à la place");
        }
        return bookLoanRepository.save(new BookLoan(bookId, request.studentId(), LocalDate.now(), request.dueDate()));
    }

    public List<BookLoan> loansForStudent(Long studentId) {
        return bookLoanRepository.findAllByStudentIdOrderByBorrowedAtDesc(studentId);
    }

    public List<BookLoan> listActiveLoans() {
        return bookLoanRepository.findAllByReturnedAtIsNull();
    }

    /** Retour d'un ouvrage — libère automatiquement la première réservation en liste d'attente, s'il y en a une. */
    @Transactional
    public BookLoan returnBook(Long loanId) {
        BookLoan loan = bookLoanRepository.findById(loanId)
                .orElseThrow(() -> ApiException.notFound("LOAN_NOT_FOUND", "Emprunt introuvable"));
        if (!loan.isActive()) {
            throw ApiException.badRequest("ALREADY_RETURNED", "Cet emprunt est déjà clôturé", List.of());
        }
        loan.markReturned(Instant.now());
        bookReservationRepository.findFirstByBookIdAndStatusOrderByReservedAtAsc(loan.getBookId(), BookReservationStatus.WAITING)
                .ifPresent(reservation -> reservation.setStatus(BookReservationStatus.FULFILLED));
        return loan;
    }

    @Transactional
    public BookReservation reserveBook(Long bookId, BookReservationCreateRequest request) {
        getBook(bookId);
        if (studentRepository.findById(request.studentId()).isEmpty()) {
            throw ApiException.notFound("STUDENT_NOT_FOUND", "Élève introuvable");
        }
        return bookReservationRepository.save(new BookReservation(bookId, request.studentId()));
    }

    public List<BookReservation> waitingList(Long bookId) {
        return bookReservationRepository.findAllByBookIdAndStatusOrderByReservedAtAsc(bookId, BookReservationStatus.WAITING);
    }
}
