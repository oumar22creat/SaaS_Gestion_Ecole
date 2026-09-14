package com.schoolsaas.parent;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {

    List<StudentParent> findAllByStudentId(Long studentId);

    List<StudentParent> findAllByParentId(Long parentId);

    Optional<StudentParent> findByStudentIdAndParentId(Long studentId, Long parentId);
}
