package com.schoolsaas.parent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParentRepository extends JpaRepository<Parent, Long> {

    /** Fiche rattachée à un compte de connexion (portail parent/élève). */
    java.util.Optional<Parent> findByUserId(Long userId);
}
