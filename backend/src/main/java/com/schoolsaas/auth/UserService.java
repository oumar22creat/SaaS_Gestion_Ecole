package com.schoolsaas.auth;

import com.schoolsaas.auth.dto.CreateUserRequest;
import com.schoolsaas.common.ApiException;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    /** Rôles disposant réellement d'écrans dans l'application (ADR-010). */
    private static final Set<Role> STAFF_ROLES = EnumSet.of(
            Role.ADMIN, Role.DIRECTION, Role.TEACHER, Role.SECRETARY, Role.VIE_SCOLAIRE, Role.ACCOUNTANT);


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crée un compte pour un membre du personnel de l'établissement courant. Le tenant n'est
     * pas un paramètre : il est posé par le contexte de requête sur l'entité tenant-scopée,
     * donc un Administrateur ne peut pas créer de compte chez un autre établissement.
     *
     * <p>Seuls les rôles du personnel sont attribuables : aucun portail élève/parent n'est
     * construit (ADR-010), un compte ÉLÈVE ou PARENT n'aurait donc aucun écran où aller.
     */
    @Transactional
    public User create(CreateUserRequest request) {
        if (!STAFF_ROLES.contains(request.role())) {
            throw ApiException.unprocessable(
                    "ROLE_NOT_ASSIGNABLE",
                    "Ce rôle ne dispose pas d'accès à l'application (voir ADR-010)");
        }
        // L'unicité de l'e-mail est par établissement (cahier-des-charges.md §21) : la requête
        // ci-dessous est déjà restreinte au tenant courant par le filtre Hibernate global.
        if (userRepository.existsByEmail(request.email())) {
            throw ApiException.conflict("EMAIL_ALREADY_USED", "Cet e-mail est déjà utilisé dans l'établissement");
        }
        return userRepository.save(new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.role()));
    }

    /**
     * Crée le compte d'un élève ou d'un parent. Volontairement séparé de {@link #create} :
     * ces deux rôles ne sont attribuables qu'ici, c'est-à-dire uniquement en étant rattaché à
     * une fiche `students` ou `parents`. Un compte PARENT sans enfant rattaché, ou un compte
     * STUDENT sans fiche élève, devient ainsi impossible à créer.
     */
    @Transactional
    public User createFamilyAccount(
            String email, String rawPassword, String firstName, String lastName, Role role) {
        if (role != Role.PARENT && role != Role.STUDENT) {
            throw ApiException.unprocessable("ROLE_NOT_ASSIGNABLE", "Ce rôle n'est pas un rôle de famille");
        }
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_USED", "Cet e-mail est déjà utilisé dans l'établissement");
        }
        return userRepository.save(
                new User(email, passwordEncoder.encode(rawPassword), firstName, lastName, role));
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

    /** Active ou désactive un compte. Un compte inactif est refusé à la connexion ET au refresh. */
    @Transactional
    public User setActive(Long id, boolean active) {
        User user = getById(id);
        refuseActingOnSelf(user, "Vous ne pouvez pas désactiver votre propre compte");
        user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public User changeRole(Long id, Role role) {
        if (!STAFF_ROLES.contains(role)) {
            throw ApiException.unprocessable(
                    "ROLE_NOT_ASSIGNABLE",
                    "Ce rôle ne dispose pas d'accès à l'application (voir ADR-010)");
        }
        User user = getById(id);
        refuseActingOnSelf(user, "Vous ne pouvez pas changer votre propre rôle");
        user.setRole(role);
        return userRepository.save(user);
    }

    /**
     * Réinitialisation par un administrateur : le nouveau mot de passe est communiqué hors
     * ligne à la personne concernée. Aucune session n'est invalidée ici — les jetons d'accès
     * déjà émis restent valides jusqu'à leur expiration (15 minutes).
     */
    @Transactional
    public User resetPassword(Long id, String rawPassword) {
        User user = getById(id);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    /**
     * Agir sur son propre compte est refusé : un administrateur qui se désactive ou se
     * rétrograde se coupe l'accès sans pouvoir revenir en arrière, et aucun outil de support
     * n'existe pour l'en sortir.
     *
     * <p>Cette seule règle suffit à empêcher un établissement de se verrouiller : l'auteur de
     * l'action est nécessairement un administrateur actif, donc il en reste toujours au moins
     * un après l'opération.
     */
    private void refuseActingOnSelf(User target, String message) {
        if (target.getId().equals(getCurrentUser().getId())) {
            throw ApiException.unprocessable("CANNOT_TARGET_SELF", message);
        }
    }

    public User getCurrentUser() {
        AuthenticatedPrincipal principal = (AuthenticatedPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return getById(principal.subjectId());
    }
}
