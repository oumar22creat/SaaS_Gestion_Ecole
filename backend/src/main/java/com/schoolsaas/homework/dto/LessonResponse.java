package com.schoolsaas.homework.dto;

import com.schoolsaas.homework.Lesson;
import java.time.LocalDate;

public record LessonResponse(
        Long id,
        Long schoolClassId,
        Long subjectId,
        LocalDate sessionDate,
        String content,
        String homework,
        LocalDate homeworkDueDate,
        Long attachmentDocumentId) {

    public static LessonResponse from(Lesson lesson) {
        return new LessonResponse(
                lesson.getId(),
                lesson.getSchoolClassId(),
                lesson.getSubjectId(),
                lesson.getSessionDate(),
                lesson.getContent(),
                lesson.getHomework(),
                lesson.getHomeworkDueDate(),
                lesson.getAttachmentDocumentId());
    }
}
