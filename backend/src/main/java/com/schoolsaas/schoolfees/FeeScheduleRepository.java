package com.schoolsaas.schoolfees;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, Long> {

    List<FeeSchedule> findAllBySchoolClassId(Long schoolClassId);

    List<FeeSchedule> findAllBySchoolClassIdAndDueDateBetween(Long schoolClassId, LocalDate from, LocalDate to);

    List<FeeSchedule> findAllByDueDateBetween(LocalDate from, LocalDate to);
}
