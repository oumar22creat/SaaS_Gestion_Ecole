package com.schoolsaas.attendance;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordChangeRepository extends JpaRepository<AttendanceRecordChange, Long> {

    List<AttendanceRecordChange> findAllByAttendanceRecordIdOrderByChangedAtDesc(Long attendanceRecordId);
}
