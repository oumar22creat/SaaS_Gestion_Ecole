package com.schoolsaas.discipline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConvocationRepository extends JpaRepository<Convocation, Long> {

    List<Convocation> findAllByStudentIdOrderByScheduledAtDesc(Long studentId);
}
