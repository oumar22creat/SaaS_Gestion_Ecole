package com.schoolsaas.portal;

import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Résout l'adresse de connexion des comptes famille.
 *
 * <p>Les fiches `students` et `parents` ne portent qu'un identifiant de compte ; c'est
 * l'adresse qui intéresse le secrétariat, puisque c'est elle qu'il dicte à une famille qui a
 * perdu son mot de passe. L'e-mail de la fiche parent ne convient pas : c'est un contact, et
 * rien n'oblige à avoir ouvert l'accès avec la même adresse.
 */
@Component
public class FamilyAccountLookup {

    private final UserRepository userRepository;

    public FamilyAccountLookup(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Adresse de connexion d'un compte, ou null si la fiche n'a pas d'accès. */
    public String emailOf(Long userId) {
        return userId == null ? null : userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    /**
     * Version groupée pour les listes : une requête pour toute la page, et non une par ligne.
     * Sur un établissement de plusieurs centaines d'élèves, la seconde approche rendait
     * l'écran inexploitable.
     */
    public Map<Long, String> emailsOf(List<Long> userIds) {
        List<Long> present = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (present.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(present).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }
}
