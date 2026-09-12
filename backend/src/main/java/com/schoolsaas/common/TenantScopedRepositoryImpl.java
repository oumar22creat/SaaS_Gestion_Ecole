package com.schoolsaas.common;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

/**
 * Classe de base pour tous les repositories (voir {@code @EnableJpaRepositories} sur
 * {@code SchoolSaasApplication}).
 *
 * <p>{@code SimpleJpaRepository#findById}/{@code #getReferenceById} délèguent à
 * {@code EntityManager#find()}/{@code #getReference()}, qui NE PASSENT PAS par les filtres
 * Hibernate ({@code @Filter}, voir {@link TenantScopedEntity} et docs/ARCHITECTURE.md
 * ADR-001) : un id valide dans un autre tenant serait sinon renvoyé tel quel, exactement le
 * scénario que CLAUDE.md règle 2 interdit — {@code TenantIsolationTest} a mis ce piège en
 * évidence. On repasse systématiquement par une requête JPQL, qui respecte le filtre actif,
 * pour que cette classe d'erreur soit structurellement impossible plutôt que de compter sur
 * chaque développeur (humain ou agent) pour s'en souvenir à chaque nouvelle entité.
 */
public class TenantScopedRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    private final EntityManager entityManager;
    private final Class<T> domainClass;

    public TenantScopedRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.domainClass = entityInformation.getJavaType();
    }

    @Override
    public Optional<T> findById(ID id) {
        String jpql = "SELECT e FROM " + domainClass.getSimpleName() + " e WHERE e.id = :id";
        return entityManager.createQuery(jpql, domainClass)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    @Override
    public T getReferenceById(ID id) {
        return findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                "Aucune entité " + domainClass.getSimpleName() + " avec l'id " + id));
    }
}
