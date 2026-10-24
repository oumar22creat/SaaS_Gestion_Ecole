package com.schoolsaas.parent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {

    List<StudentParent> findAllByStudentId(Long studentId);

    List<StudentParent> findAllByParentId(Long parentId);

    List<StudentParent> findAllByParentIdIn(Collection<Long> parentIds);

    List<StudentParent> findAllByStudentIdIn(Collection<Long> studentIds);

    Optional<StudentParent> findByStudentIdAndParentId(Long studentId, Long parentId);
}
