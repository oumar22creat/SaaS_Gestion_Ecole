package com.schoolsaas.homework;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllBySchoolClassIdAndSessionDateBetween(Long schoolClassId, LocalDate from, LocalDate to);
}
