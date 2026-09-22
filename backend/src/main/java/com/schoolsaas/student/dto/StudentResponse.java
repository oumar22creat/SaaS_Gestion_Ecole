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
        boolean active,
        /**
         * Vrai si un compte de connexion au portail est ouvert. Booléen et non l'identifiant
         * du compte : l'écran a besoin de savoir si l'accès existe, pas de le désigner.
         */
        boolean hasPortalAccess) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getBirthDate(),
                student.getGender(),
                student.getSchoolClassId(),
                student.isActive(),
                student.getUserId() != null);
    }
}
