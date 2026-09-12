package com.schoolsaas.auth;

import com.schoolsaas.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Recherche par identifiant, restreinte au tenant courant par le filtre Hibernate global
     * (voir com.schoolsaas.common.TenantScopedEntity) : un id valide dans un autre
     * établissement renvoie ici "introuvable", jamais les données d'un autre tenant (voir
     * cahier-des-charges.md §2.2).
     */
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Utilisateur introuvable"));
    }

    public Page<User> list(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getCurrentUser() {
        AuthenticatedPrincipal principal = (AuthenticatedPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return getById(principal.subjectId());
    }
}
