package com.schoolsaas.library;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByBarcode(String barcode);

    List<Book> findAllByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
            String title, String author, String barcode);
}
