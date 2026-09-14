package com.schoolsaas.library.dto;

import com.schoolsaas.library.Book;

public record BookResponse(Long id, String barcode, String isbn, String title, String author, int totalCopies, int availableCopies) {

    public static BookResponse from(Book book, int availableCopies) {
        return new BookResponse(book.getId(), book.getBarcode(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getTotalCopies(), availableCopies);
    }
}
