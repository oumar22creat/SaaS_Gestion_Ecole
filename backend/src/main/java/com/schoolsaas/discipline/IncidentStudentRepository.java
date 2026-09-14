package com.schoolsaas.discipline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentStudentRepository extends JpaRepository<IncidentStudent, Long> {

    List<IncidentStudent> findAllByIncidentId(Long incidentId);

    List<IncidentStudent> findAllByStudentId(Long studentId);
}
