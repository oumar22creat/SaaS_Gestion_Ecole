package com.schoolsaas.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Le risque couvert ici est un déploiement où `JWT_SECRET` a été oublié : sans ce garde-fou
 * l'application démarre avec la clé publique du dépôt, et n'importe qui peut forger un jeton
 * d'administrateur sans que rien ne le signale.
 */
class JwtSecretGuardTest {

    private JwtSecretGuard guard(String profile, String secret) {
        MockEnvironment environment = new MockEnvironment();
        if (profile != null) {
            environment.setActiveProfiles(profile);
        }
        return new JwtSecretGuard(new JwtProperties(secret, 15, 30), environment);
    }

    @Test
    void refusesToStartInProductionWithTheRepositorySecret() {
        assertThatThrownBy(() -> guard("prod", JwtSecretGuard.DEVELOPMENT_SECRET).verifySecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void refusesAnEmptyOrTooShortSecretOutsideDevelopment() {
        assertThatThrownBy(() -> guard("prod", "").verifySecret()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> guard("staging", "trop-court").verifySecret())
                .isInstanceOf(IllegalStateException.class);
    }

    /** Sans profil explicite, on ne peut pas conclure au développement : la règle s'applique. */
    @Test
    void appliesTheRuleWhenNoProfileIsDeclared() {
        assertThatThrownBy(() -> guard(null, JwtSecretGuard.DEVELOPMENT_SECRET).verifySecret())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void leavesDevelopmentUsableAfterAPlainClone() {
        assertThatCode(() -> guard("dev", JwtSecretGuard.DEVELOPMENT_SECRET).verifySecret())
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsARealSecretInProduction() {
        assertThatCode(() -> guard("prod", "k7Qm2vY8pR4tLz1nJ6wX3bH9cF5dG0sA").verifySecret())
                .doesNotThrowAnyException();
    }
}
