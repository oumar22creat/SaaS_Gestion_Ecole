package com.schoolsaas.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findBySubdomain(String subdomain);

    boolean existsBySubdomain(String subdomain);

    Optional<Tenant> findByCustomDomain(String customDomain);

    long countByStatus(TenantStatus status);
}
