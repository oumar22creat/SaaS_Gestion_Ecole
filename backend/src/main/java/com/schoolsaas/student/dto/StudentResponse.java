package com.schoolsaas.student.dto;

import com.schoolsaas.student.Student;
import java.time.LocalDate;

public record StudentResponse(
        Long id,
        String studentNumber,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        Long schoolClassId,
        boolean active) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getBirthDate(),
                student.getGender(),
                student.getSchoolClassId(),
                student.isActive());
    }
}
