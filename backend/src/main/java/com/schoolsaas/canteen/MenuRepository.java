package com.schoolsaas.canteen;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByDate(LocalDate date);

    List<Menu> findAllByDateBetweenOrderByDate(LocalDate from, LocalDate to);
}
