package com.schoolsaas.mobilemoney;

/**
 * Frontière avec l'opérateur de mobile money (Orange Money Mali en cible, cahier §4.3).
 *
 * <p>Séparée de son implémentation pour la même raison que {@code StripeGateway} : les tests
 * doivent pouvoir simuler l'opérateur sans appel réseau, et brancher le vrai fournisseur ne
 * doit toucher aucun appelant.
 *
 * <p><b>Aucune implémentation réseau n'existe à ce jour.</b> L'API Orange Money Web Payment
 * exige un compte marchand, un client_id/client_secret et une clé marchand ; tant qu'ils ne
 * sont pas disponibles, {@link LoggingMobileMoneyGateway} se contente de journaliser. Écrire
 * l'appel HTTP à l'aveugle, sans documentation ni identifiants pour valider les schémas,
 * produirait du code d'apparence terminée qui échouerait en production.
 */
public interface MobileMoneyGateway {

    /** Nom du fournisseur, stocké sur la tentative pour tracer qui a traité quoi. */
    String providerName();

    /**
     * Demande un paiement au payeur. L'opérateur pousse une confirmation sur son téléphone,
     * puis rappelle notre callback : la réponse ici n'est donc jamais un paiement abouti.
     */
    InitiationResult initiate(String reference, long amountCents, String payerMsisdn, String description);

    /**
     * @param providerReference identifiant de la transaction chez l'opérateur, s'il est déjà connu
     * @param paymentUrl page de paiement à ouvrir, pour les fournisseurs qui en exposent une
     */
    record InitiationResult(String providerReference, String paymentUrl) {
    }
}
