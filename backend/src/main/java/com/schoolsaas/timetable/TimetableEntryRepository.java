package com.schoolsaas.timetable;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findAllByDayOfWeekAndTeacherId(DayOfWeek dayOfWeek, Long teacherId);

    List<TimetableEntry> findAllByDayOfWeekAndRoomId(DayOfWeek dayOfWeek, Long roomId);

    List<TimetableEntry> findAllByDayOfWeekAndSchoolClassId(DayOfWeek dayOfWeek, Long schoolClassId);

    List<TimetableEntry> findAllBySchoolClassId(Long schoolClassId);

    List<TimetableEntry> findAllByTeacherId(Long teacherId);
}
