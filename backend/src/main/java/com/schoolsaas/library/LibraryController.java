package com.schoolsaas.library;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.library.dto.BookCreateRequest;
import com.schoolsaas.library.dto.BookLoanCreateRequest;
import com.schoolsaas.library.dto.BookLoanResponse;
import com.schoolsaas.library.dto.BookReservationCreateRequest;
import com.schoolsaas.library.dto.BookReservationResponse;
import com.schoolsaas.library.dto.BookResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Bibliothèque — cahier-des-charges.md §19.3, ROADMAP.md 3.5. */
@RestController
@RequestMapping("/api/v1/library")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY')")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<BookResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        Book book = libraryService.createBook(request);
        return ApiResponse.of(BookResponse.from(book, libraryService.availableCopies(book.getId())));
    }

    @GetMapping("/books")
    public ApiResponse<List<BookResponse>> search(@RequestParam String query) {
        List<BookResponse> data = libraryService.search(query).stream()
                .map(book -> BookResponse.from(book, libraryService.availableCopies(book.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/books/{id}")
    public ApiResponse<BookResponse> getBook(@PathVariable Long id) {
        Book book = libraryService.getBook(id);
        return ApiResponse.of(BookResponse.from(book, libraryService.availableCopies(id)));
    }

    @PostMapping("/books/{id}/loans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<BookLoanResponse> borrowBook(@PathVariable Long id, @Valid @RequestBody BookLoanCreateRequest request) {
        return ApiResponse.of(BookLoanResponse.from(libraryService.borrowBook(id, request), LocalDate.now()));
    }

    @PostMapping("/loans/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<BookLoanResponse> returnBook(@PathVariable Long id) {
        return ApiResponse.of(BookLoanResponse.from(libraryService.returnBook(id), LocalDate.now()));
    }

    @GetMapping("/students/{studentId}/loans")
    public ApiResponse<List<BookLoanResponse>> loansForStudent(@PathVariable Long studentId) {
        LocalDate today = LocalDate.now();
        List<BookLoanResponse> data = libraryService.loansForStudent(studentId).stream()
                .map(loan -> BookLoanResponse.from(loan, today))
                .toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/loans/overdue")
    public ApiResponse<List<BookLoanResponse>> overdueLoans() {
        LocalDate today = LocalDate.now();
        List<BookLoanResponse> data = libraryService.listActiveLoans().stream()
                .map(loan -> BookLoanResponse.from(loan, today))
                .filter(BookLoanResponse::overdue)
                .toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/books/{id}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<BookReservationResponse> reserveBook(@PathVariable Long id, @Valid @RequestBody BookReservationCreateRequest request) {
        BookReservation reservation = libraryService.reserveBook(id, request);
        return ApiResponse.of(BookReservationResponse.from(reservation, libraryService.waitingList(id).size()));
    }

    @GetMapping("/books/{id}/reservations")
    public ApiResponse<List<BookReservationResponse>> waitingList(@PathVariable Long id) {
        List<BookReservation> waiting = libraryService.waitingList(id);
        List<BookReservationResponse> data = new ArrayList<>();
        for (int i = 0; i < waiting.size(); i++) {
            data.add(BookReservationResponse.from(waiting.get(i), i + 1));
        }
        return ApiResponse.of(data);
    }
}
