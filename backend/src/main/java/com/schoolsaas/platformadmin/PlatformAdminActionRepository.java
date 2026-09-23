package com.schoolsaas.platformadmin;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAdminActionRepository extends JpaRepository<PlatformAdminAction, Long> {

    Page<PlatformAdminAction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<PlatformAdminAction> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
