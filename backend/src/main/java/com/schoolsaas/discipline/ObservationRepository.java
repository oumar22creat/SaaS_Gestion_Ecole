package com.schoolsaas.discipline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<Observation, Long> {

    List<Observation> findAllByStudentIdOrderByCreatedAtDesc(Long studentId);
}
