package com.schoolsaas.timetable.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Créneau d'emploi du temps, avec les libellés nécessaires à son affichage.
 *
 * <p>Les libellés ({@code schoolClassName}, {@code subjectName}, {@code teacherName},
 * {@code roomName}) évitent au client d'interroger les référentiels — en particulier
 * {@code GET /teachers}, réservé à ADMIN/DIRECTION : un enseignant tombait en 403 en ouvrant
 * son emploi du temps sur mobile. Seuls le prénom et le nom de l'enseignant sont exposés, jamais
 * son e-mail ni son téléphone. Un libellé est {@code null} si la ressource liée est introuvable ;
 * {@code roomName} l'est aussi quand le créneau n'a pas de salle. Construit par
 * {@link com.schoolsaas.timetable.TimetableEntryMapper}.
 */
public record TimetableEntryResponse(
        Long id,
        Long schoolClassId,
        String schoolClassName,
        Long subjectId,
        String subjectName,
        Long teacherId,
        String teacherName,
        Long roomId,
        String roomName,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {
}
