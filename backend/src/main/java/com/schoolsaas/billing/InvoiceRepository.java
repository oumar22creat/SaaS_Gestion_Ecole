package com.schoolsaas.billing;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByStripeInvoiceId(String stripeInvoiceId);

    /**
     * Filtrage manuel par tenant_id : {@code Invoice} n'est pas une {@code TenantScopedEntity}
     * (voir docs/ARCHITECTURE.md ADR-009), donc le filtre Hibernate global ne s'applique pas
     * ici — c'est cette méthode qui garantit qu'un tenant ne voit jamais les factures d'un
     * autre (voir CLAUDE.md, règle sur les tests d'isolation).
     */
    Page<Invoice> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
}
