package com.schoolsaas.portal.dto;

import java.time.LocalDate;

/**
 * Réponses du portail parent/élève. Volontairement plus pauvres que les DTO du personnel :
 * une famille n'a pas à recevoir les commentaires internes ni les identifiants techniques
 * qui ne lui servent pas.
 */
public final class PortalResponse {

    private PortalResponse() {
    }

    public record Child(
            Long id, String firstName, String lastName, String studentNumber, Long schoolClassId) {
    }

    public record GradeLine(
            Long examId,
            String label,
            LocalDate examDate,
            Double score,
            double maxScore,
            int coefficient,
            boolean absent) {
    }

    public record AttendanceLine(LocalDate date, String status, String reason, boolean justified) {
    }

    /** Créneau de l'emploi du temps de la classe de l'élève. */
    public record TimetableSlot(
            String dayOfWeek,
            String startTime,
            String endTime,
            String subject,
            String teacher,
            String room) {
    }
}
