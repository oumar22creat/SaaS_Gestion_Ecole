package com.schoolsaas.reportcard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportCardRepository extends JpaRepository<ReportCard, Long> {

    Optional<ReportCard> findByStudentIdAndPeriodLabel(Long studentId, String periodLabel);

    List<ReportCard> findAllByStudentId(Long studentId);

    List<ReportCard> findAllBySchoolClassIdAndPeriodLabel(Long schoolClassId, String periodLabel);
}
