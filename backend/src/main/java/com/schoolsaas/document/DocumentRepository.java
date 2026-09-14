package com.schoolsaas.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByArchivedFalseAndScopeAndSubjectId(DocumentScope scope, Long subjectId);

    List<Document> findAllByArchivedFalseAndScopeAndSchoolClassId(DocumentScope scope, Long schoolClassId);

    List<Document> findAllByArchivedFalseAndScopeAndServiceLabel(DocumentScope scope, String serviceLabel);
}
