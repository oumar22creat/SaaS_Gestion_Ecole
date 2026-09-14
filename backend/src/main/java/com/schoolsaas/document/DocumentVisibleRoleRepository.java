package com.schoolsaas.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentVisibleRoleRepository extends JpaRepository<DocumentVisibleRole, Long> {

    List<DocumentVisibleRole> findAllByDocumentId(Long documentId);
}
