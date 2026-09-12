package com.schoolsaas.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Active le filtre Hibernate {@code tenantFilter} et la variable de session PostgreSQL
 * utilisée par les politiques RLS, pour le tenant courant.
 *
 * <p>Doit tourner dans une transaction réelle : Spring ne résout l'{@link EntityManager}
 * "partagé" injecté par {@code @PersistenceContext} vers l'EntityManager de la requête
 * (ouvert par {@code OpenEntityManagerInViewFilter}) que si une synchronisation
 * transactionnelle est active — sinon chaque appel obtient un EntityManager temporaire
 * différent, jeté immédiatement après, et l'activation du filtre n'a aucun effet sur les
 * requêtes suivantes. C'est exactement le bug mis en évidence par
 * {@code TenantIsolationTest} lorsque cette logique vivait directement dans
 * {@link TenantContextInterceptor} (hors transaction). Cette classe est appelée par
 * l'intercepteur depuis un bean distinct pour bénéficier du proxy {@code @Transactional} de
 * Spring (l'auto-invocation depuis la même classe ne serait pas interceptée).
 */
@Component
public class TenantSessionConfigurer {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRED)
    public void applyTenant(Long tenantId) {
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
        entityManager.createNativeQuery("SELECT set_config('app.tenant_id', :tenantId, false)")
                .setParameter("tenantId", String.valueOf(tenantId))
                .getSingleResult();
    }
}
