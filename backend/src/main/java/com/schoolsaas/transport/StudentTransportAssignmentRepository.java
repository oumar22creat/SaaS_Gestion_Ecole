package com.schoolsaas.transport;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentTransportAssignmentRepository extends JpaRepository<StudentTransportAssignment, Long> {

    Optional<StudentTransportAssignment> findByStudentId(Long studentId);
}
