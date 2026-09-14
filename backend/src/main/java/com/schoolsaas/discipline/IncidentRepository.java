package com.schoolsaas.discipline;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findAllBySchoolClassIdAndOccurredAtBetween(Long schoolClassId, LocalDate from, LocalDate to);

    List<Incident> findAllByOccurredAtBetween(LocalDate from, LocalDate to);
}
