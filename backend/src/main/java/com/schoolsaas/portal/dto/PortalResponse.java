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

    /**
     * Une échéance de frais telle qu'une famille la lit : ce qu'elle doit, ce qu'elle a déjà
     * versé, et si la date est passée. Aucun identifiant de facture — il ne lui sert à rien,
     * et le personnel garde la main sur l'encaissement.
     */
    public record FeeLine(
            String label,
            LocalDate dueDate,
            long amountDueCents,
            long amountPaidCents,
            long amountRemainingCents,
            String status,
            boolean overdue) {
    }

    /** Ce que la famille doit au total pour cet élève, tous frais confondus. */
    public record FeeSummary(
            long totalDueCents,
            long totalPaidCents,
            long totalRemainingCents,
            long overdueCents,
            String currency,
            java.util.List<FeeLine> lines) {
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
