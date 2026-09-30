package com.schoolsaas.onboarding.dto;

import java.util.List;

/**
 * Avancement de la configuration initiale d'un établissement (ROADMAP.md 1.3).
 *
 * <p>L'API ne renvoie que des clés et des comptages : les libellés et les liens vivent dans
 * l'interface, qui seule sait vers quel écran envoyer l'utilisateur.
 */
public record OnboardingStatusResponse(List<Step> steps, boolean complete) {

    /**
     * `key` identifie l'étape ; `count` sert à afficher l'avancement réel, pas juste
     * coché/non coché.
     *
     * <p>`required` distingue ce dont l'établissement ne peut pas se passer de ce qui lui est
     * simplement proposé. Les salles, par exemple : utiles à qui fait circuler ses classes,
     * sans objet pour qui attribue une salle fixe par classe. Les compter comme obligatoires
     * laisserait le guide inachevé à jamais chez ce dernier.
     */
    public record Step(String key, long count, boolean done, boolean required) {

        public static Step of(String key, long count) {
            return new Step(key, count, count > 0, true);
        }

        public static Step optionnelle(String key, long count) {
            return new Step(key, count, count > 0, false);
        }
    }
}
