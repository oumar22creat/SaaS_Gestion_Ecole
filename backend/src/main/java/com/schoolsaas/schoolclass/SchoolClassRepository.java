package com.schoolsaas.schoolclass;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    /** Classes dont l'un de ces enseignants est professeur principal (périmètre enseignant). */
    List<SchoolClass> findAllByHeadTeacherIdIn(Collection<Long> headTeacherIds);

    Page<SchoolClass> findAllByIdIn(Collection<Long> ids, Pageable pageable);
}
