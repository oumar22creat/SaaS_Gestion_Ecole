package com.schoolsaas.reportcard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ReportCardEntryRepository extends JpaRepository<ReportCardEntry, Long> {

    List<ReportCardEntry> findAllByReportCardId(Long reportCardId);

    Optional<ReportCardEntry> findByReportCardIdAndSubjectId(Long reportCardId, Long subjectId);

    /**
     * Requête de suppression en masse (pas {@code deleteAllByReportCardId} dérivé de
     * Spring Data, qui charge puis appelle {@code entityManager.remove()} par ligne — ce
     * DELETE reste alors en attente de flush tandis que les nouvelles lignes, insérées
     * immédiatement à cause de {@code GenerationType.IDENTITY}, violaient la contrainte
     * UNIQUE(report_card_id, subject_id) en régénérant un bulletin déjà existant).
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from ReportCardEntry e where e.reportCardId = :reportCardId")
    void deleteAllByReportCardId(Long reportCardId);
}
