package com.schoolsaas.discipline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservationRepository extends JpaRepository<Observation, Long> {

    List<Observation> findAllByStudentIdOrderByCreatedAtDesc(Long studentId);

    /** Les observations de toute une classe d'un coup, plutôt qu'une requête par élève. */
    List<Observation> findAllByStudentIdIn(List<Long> studentIds);
}
