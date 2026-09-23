package com.schoolsaas.student.dto;

import com.schoolsaas.student.Student;
import java.time.LocalDate;

/**
 * @param portalEmail adresse de connexion au portail mobile, ou null si aucun accès n'est
 *                    ouvert. Sa présence tient lieu d'indicateur d'accès, et elle porte
 *                    l'information dont le secrétariat a réellement besoin : ce qu'il dicte à
 *                    une famille qui a perdu son mot de passe.
 */
public record StudentResponse(
        Long id,
        String studentNumber,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        Long schoolClassId,
        boolean active,
        String portalEmail) {

    public static StudentResponse from(Student student, String portalEmail) {
        return new StudentResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFirstName(),
                student.getLastName(),
                student.getBirthDate(),
                student.getGender(),
                student.getSchoolClassId(),
                student.isActive(),
                portalEmail);
    }
}
