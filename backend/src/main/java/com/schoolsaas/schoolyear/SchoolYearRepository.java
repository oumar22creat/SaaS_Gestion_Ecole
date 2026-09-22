package com.schoolsaas.schoolyear;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolYearRepository extends JpaRepository<SchoolYear, Long> {

    Optional<SchoolYear> findByStatus(SchoolYearStatus status);

    Optional<SchoolYear> findByLabel(String label);

    List<SchoolYear> findAllByOrderByStartDateDesc();
}
