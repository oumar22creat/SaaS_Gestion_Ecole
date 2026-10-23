package com.schoolsaas.timetable;

import java.time.DayOfWeek;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findAllByDayOfWeekAndTeacherId(DayOfWeek dayOfWeek, Long teacherId);

    List<TimetableEntry> findAllByDayOfWeekAndRoomId(DayOfWeek dayOfWeek, Long roomId);

    List<TimetableEntry> findAllByDayOfWeekAndSchoolClassId(DayOfWeek dayOfWeek, Long schoolClassId);

    List<TimetableEntry> findAllBySchoolClassId(Long schoolClassId);

    List<TimetableEntry> findAllByTeacherId(Long teacherId);

    /** Créneaux d'un ensemble de fiches enseignant (périmètre enseignant, voir TeacherScope). */
    List<TimetableEntry> findAllByTeacherIdIn(Collection<Long> teacherIds);
}
