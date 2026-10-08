package com.schoolsaas.student.dto;

import com.schoolsaas.student.Student;
import java.time.LocalDate;

/**
 * @param hasPhoto    un portrait est enregistré. Le booléen plutôt que l'image : une liste
 *                    de mille élèves ne va pas transporter mille photos pour afficher un
 *                    tableau, et l'écran qui en a besoin la demande élève par élève.
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
        boolean hasPhoto,
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
                student.getPhotoStorageKey() != null,
                portalEmail);
    }
}
