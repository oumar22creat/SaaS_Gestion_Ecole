package com.schoolsaas.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByStudentIdAndDate(Long studentId, LocalDate date);

    List<AttendanceRecord> findAllBySchoolClassIdAndDate(Long schoolClassId, LocalDate date);

    List<AttendanceRecord> findAllByStudentIdAndDateBetween(Long studentId, LocalDate from, LocalDate to);

    List<AttendanceRecord> findAllByDateBetween(LocalDate from, LocalDate to);
}
