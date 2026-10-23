package com.schoolsaas.homework;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findAllBySchoolClassIdAndSessionDateBetween(Long schoolClassId, LocalDate from, LocalDate to);

    /** Séances des classes d'un enseignant (liste non filtrée vue par un TEACHER). */
    Page<Lesson> findAllBySchoolClassIdIn(Collection<Long> schoolClassIds, Pageable pageable);
}
