package com.schoolsaas.discipline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<Sanction, Long> {

    List<Sanction> findAllByIncidentId(Long incidentId);

    List<Sanction> findAllByStudentIdOrderByDecidedAtDesc(Long studentId);

    long countByType(SanctionType type);
}
