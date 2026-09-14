package com.schoolsaas.teacher.dto;

import com.schoolsaas.teacher.Teacher;

public record TeacherResponse(
        Long id, String firstName, String lastName, String email, String phone, boolean active) {

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
                teacher.getId(),
                teacher.getFirstName(),
                teacher.getLastName(),
                teacher.getEmail(),
                teacher.getPhone(),
                teacher.isActive());
    }
}
