package com.schoolsaas.sms;

/**
 * Envoi effectif d'un SMS. Frontière volontairement séparée du service, même pattern que
 * {@code StripeGateway} et {@code MobileMoneyGateway} : brancher un opérateur reste un
 * remplacement d'implémentation, pas un changement d'appelants.
 */
public interface SmsGateway {

    /**
     * @return la référence du fournisseur si l'envoi est accepté
     * @throws SmsDeliveryException si le fournisseur refuse ou reste injoignable
     */
    String send(String recipient, String body);

    /** Nom du fournisseur, tracé sur chaque envoi pour le rapprochement de facture. */
    String providerName();

    /** Exception d'envoi, traitée par le service qui trace l'échec. */
    class SmsDeliveryException extends RuntimeException {
        public SmsDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }

        public SmsDeliveryException(String message) {
            super(message);
        }
    }
}
