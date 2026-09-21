package com.schoolsaas.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Le filtre Hibernate tenantFilter (activé par TenantContextInterceptor) restreint déjà
    // implicitement cette requête au tenant courant — pas besoin de school_id explicite ici
    // (voir com.schoolsaas.common.TenantScopedEntity).
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findAllByActiveTrue();

}
