package com.schoolsaas.schoolyear.dto;

import com.schoolsaas.schoolyear.Enrollment;
import com.schoolsaas.schoolyear.EnrollmentStatus;
import java.time.LocalDate;

/** Une inscription, enrichie des noms pour être lisible sans autre appel. */
public record EnrollmentResponse(
        Long id,
        Long schoolYearId,
        String schoolYearLabel,
        Long studentId,
        String studentName,
        String studentNumber,
        Long schoolClassId,
        String className,
        EnrollmentStatus status,
        LocalDate enrolledAt,
        LocalDate leftAt) {

    public static EnrollmentResponse from(
            Enrollment enrollment,
            String schoolYearLabel,
            String studentName,
            String studentNumber,
            String className) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getSchoolYearId(),
                schoolYearLabel,
                enrollment.getStudentId(),
                studentName,
                studentNumber,
                enrollment.getSchoolClassId(),
                className,
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getLeftAt());
    }
}
